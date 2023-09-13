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
        .map( postJson => new Post(postJson, config.contentTransformer) )
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

    val actor = ujson.read(os.read.stream(actorJsonPath) )

    val userDisplayName = config.overrideDisplayName.getOrElse( actor.obj("name").str )

    val mbUserHost =
      actor.obj("id").str match
        case UserIdUrlRegex(host, user) => Some(UserHost(user,host))
        case _ => None

    val mainTitle = config.mainTitle.getOrElse {
      mbUserHost.fold("Mastodon archive")(uh => s"Mastodon archive: ${uh}")
    }

    FossilphantContext( config, mbUserHost, mainTitle, userDisplayName, reverseChronologicalPublicPosts, reverseChronologicalPublicPostsNoReplies, publicPostsByLocalId, threadNexts, outbox.obj )
  end apply

case class FossilphantContext(
  config : FossilphantConfig,
  mbUserHost : Option[UserHost],
  mainTitle : String,
  userDisplayName : String,
  reverseChronologicalPublicPosts : Seq[Post],
  reverseChronologicalPublicPostsNoReplies : Seq[Post],
  publicPostsByLocalId : Map[String,Post],
  threadNexts : Map[String,String],
  rawOutbox : UjsonObjValue
):
  lazy val pagesIncludingReplies = reverseChronologicalPublicPosts.grouped( config.pageLength ).toSeq
  lazy val pagesNoReplies = reverseChronologicalPublicPostsNoReplies.grouped( config.pageLength ).toSeq
