package com.mchange.fossilphant

import scala.collection.*

object FossilphantContext:
  def apply( archiveDir : os.Path, config : FossilphantConfig) : FossilphantContext =
    val outboxJsonPath = archiveDir / "outbox.json"
    val actorJsonPath  = archiveDir / "actor.json"

    // FIXME: we are very brittle-ly assuming everything goes right...
    val outbox = ujson.read(os.read.stream(outboxJsonPath) )
    val items = outbox.obj("orderedItems").arr.map( _.obj )
    val grouped = items.groupBy( _("type").str ) // expect "Announce", "Create"
    val unsortedPostJsons = grouped("Create")

    val publicPosts =
      unsortedPostJsons
        .map( postJson => new MastodonPost(postJson, config.contentTransformer) )
        .filter( effectivelyPublic(config) )

    val publicPostsByLocalId = publicPosts.map( post => (post.localId, post)).toMap

    val threadNexts =
      publicPosts.view
        .map( post => (post.localId, post.inReplyTo)  )
        .collect { case (lid, InReplyTo.Self(prev)) => (prev,lid)}
        .toMap

    val reverseChronologicalPublicPosts =
      immutable.SortedSet.from( publicPosts ).toSeq

    val reverseChronologicalPublicPostsNoReplies =
      reverseChronologicalPublicPosts.filter( _.inReplyTo == InReplyTo.NoOne )

    val reverseChronologicalPublicPostsWithRepliesToOthersOnly =
      reverseChronologicalPublicPosts.filterNot( _.inReplyTo.isInstanceOf[InReplyTo.Self] )

    val actor = ujson.read(os.read.stream(actorJsonPath) )

    val userDisplayName = config.overrideDisplayName.getOrElse( actor.obj("name").str )

    val mbUserHost =
      actor.obj("id").str match
        case UserIdUrlRegex(host, user) => Some(UserHost(user,host))
        case _ => None

    val mainTitle = config.mainTitle.getOrElse {
      mbUserHost.fold("Mastodon archive")(uh => s"Mastodon archive: ${uh}")
    }

    FossilphantContext(
      config,
      mbUserHost,
      mainTitle,
      userDisplayName,
      reverseChronologicalPublicPosts,
      reverseChronologicalPublicPostsNoReplies,
      reverseChronologicalPublicPostsWithRepliesToOthersOnly,
      publicPostsByLocalId,
      threadNexts,
      outbox.obj )
  end apply

case class FossilphantContext(
  config : FossilphantConfig,
  mbUserHost : Option[UserHost],
  mainTitle : String,
  userDisplayName : String,
  reverseChronologicalPublicPosts : Seq[Post],
  reverseChronologicalPublicPostsNoReplies : Seq[Post],
  reverseChronologicalPublicPostsWithRepliesToOthersOnly : Seq[Post], // when threads will be followed and catch self-replies
  publicPostsByLocalId : Map[String,Post],
  threadNexts : Map[String,String],
  rawOutbox : UjsonObjValue
):
  lazy val pagesIncludingReplies = reverseChronologicalPublicPosts.grouped( config.pageLength ).toSeq
  lazy val pagesIncludingRepliesToOthersOnly = reverseChronologicalPublicPostsWithRepliesToOthersOnly.grouped( config.pageLength ).toSeq
  lazy val pagesNoReplies = reverseChronologicalPublicPostsNoReplies.grouped( config.pageLength ).toSeq
  lazy val defaultTagLine : String =
    val earliest = reverseChronologicalPublicPosts.last.published
    val latest = reverseChronologicalPublicPosts.head.published
    val formatter = TimestampDateFormatter
    def fdate( i : java.time.Instant ) : String = formatter.format(i.atZone(config.timestampTimezone))
    s"from ${fdate(earliest)} to ${fdate(latest)}"
  def tagline : String = config.mainTagline.getOrElse( defaultTagLine )
