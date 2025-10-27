package com.mchange.fossilphant

import scala.collection.*
import com.mchange.bskyarchive.*

object FossilphantContext:
  def forMastodon( archiveDir : os.Path, config : FossilphantConfig) : FossilphantContext =
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
      threadNexts
    )
  end forMastodon

  def forBluesky( bskyArchive : BskyArchive, config : FossilphantConfig, userHandleNoAt : Option[String], imageDir : os.Path ) : FossilphantContext =
    import com.mchange.fossilphant.bluesky.*
    val did = bskyArchive.did
    val publicPosts =
      bskyArchive.posts().map: carBlockPost =>
        val tid = bskyArchive.getTid( carBlockPost ).getOrElse( throw new MissingTid( s"Could not find tid for record: $carBlockPost" ) )
        // println( s"tid: $tid" )
        BlueskyPost( carBlockPost, tid, did, bskyArchive.tidToCid, userHandleNoAt, config.contentTransformer )
    val publicPostsByLocalId =
      publicPosts.map( post => (post.localId, post)).toMap
    val threadNexts =
      publicPosts.view
        .map( post => (post.localId, post.inReplyTo)  )
        .collect { case (lid, InReplyTo.Self(prev)) => (prev,lid)}
        .toMap

    /* debug only */
    //println( "threadNexts:" )
    //threadNexts.foreach( println )
    //val prevs = publicPosts.map( _.inReplyTo ).collect { case InReplyTo.Self(prev) => prev }
    //prevs.foreach( println )
    /* end debug only */

    val userDisplayName = config.overrideDisplayName.getOrElse( bskyArchive.profile.cbor.get("displayName").AsString )
    val mbUserHost = Some(UserHost(userHandleNoAt.fold(did)("@"+_),"bsky.app"))
    val mainTitle = config.mainTitle.getOrElse:
      mbUserHost.fold("Bluesky archive")(uh => s"Bluesky archive: ${uh.user}")
    val reverseChronologicalPublicPosts =
      immutable.SortedSet.from( publicPosts ).toSeq
    val reverseChronologicalPublicPostsNoReplies =
      reverseChronologicalPublicPosts.filter( _.inReplyTo == InReplyTo.NoOne )
    val reverseChronologicalPublicPostsWithRepliesToOthersOnly =
      reverseChronologicalPublicPosts.filterNot( _.inReplyTo.isInstanceOf[InReplyTo.Self] )

    FossilphantContext(
      config,
      mbUserHost,
      mainTitle,
      userDisplayName,
      reverseChronologicalPublicPosts,
      reverseChronologicalPublicPostsNoReplies,
      reverseChronologicalPublicPostsWithRepliesToOthersOnly,
      publicPostsByLocalId,
      threadNexts
    )

  end forBluesky

case class FossilphantContext(
  config : FossilphantConfig,
  mbUserHost : Option[UserHost],
  mainTitle : String,
  userDisplayName : String,
  reverseChronologicalPublicPosts : Seq[Post],
  reverseChronologicalPublicPostsNoReplies : Seq[Post],
  reverseChronologicalPublicPostsWithRepliesToOthersOnly : Seq[Post], // when threads will be followed and catch self-replies
  publicPostsByLocalId : Map[String,Post],
  threadNexts : Map[String,String]
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
