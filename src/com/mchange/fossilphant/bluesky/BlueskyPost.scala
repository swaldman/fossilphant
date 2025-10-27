package com.mchange.fossilphant.bluesky

import com.mchange.fossilphant.*

import com.mchange.fossilphant.Post.PollItem

import java.time.Instant
import scala.collection.*
import scala.util.{Failure, Success, Try}
import unstatic.UrlPath.*

import com.mchange.bskyarchive.BlobRef
import com.mchange.bskyarchive.CarBlock
import com.mchange.bskyarchive.Cid
import com.mchange.bskyarchive.Embed
import com.mchange.bskyarchive.Facet
import java.nio.charset.StandardCharsets

object BlueskyPost:
  given ReverseChronologicalPublished : Ordering[BlueskyPost] with
    def compare( x : BlueskyPost, y : BlueskyPost ) : Int = Post.ReverseChronologicalPublished.compare( x, y )
  private val UTF8 = StandardCharsets.UTF_8
  private val EndAnchor = "</a>".getBytes(UTF8)
  def applyLink(link : Facet.Link, facetTarget : Array[Array[Byte]]) : Array[Array[Byte]] =
    val (start, end) = (link.byteStart, link.byteEnd)
    facetTarget(start) = s"""<a target="_blank" href="${link.uri}">""".getBytes(UTF8) ++ facetTarget(start)
    facetTarget(end-1) = facetTarget(end-1) ++ EndAnchor
    facetTarget
  def applyMention(mention : Facet.Mention, facetTarget : Array[Array[Byte]]) : Array[Array[Byte]] =
    val (start, end) = (mention.byteStart, mention.byteEnd)
    facetTarget(start) = s"""<a target="_blank" href="https://bsky.app/profile/${mention.did}">""".getBytes(UTF8) ++ facetTarget(start)
    facetTarget(end-1) = facetTarget(end-1) ++ EndAnchor
    facetTarget
  def applyTag(tag : Facet.Tag, facetTarget : Array[Array[Byte]]) : Array[Array[Byte]] =
    val (start, end) = (tag.byteStart, tag.byteEnd)
    facetTarget(start) = s"""<a target="_blank" href="https://bsky.app/hashtag/${tag.tag}">""".getBytes(UTF8) ++ facetTarget(start)
    facetTarget(end-1) = facetTarget(end-1) ++ EndAnchor
    facetTarget
  def applyFacets( rawContent : String, facets : Set[Facet] ) : String =
    if facets.isEmpty then
      rawContent
    else
      val target = rawContent.getBytes(UTF8).map( b => Array(b) ).toArray
      facets.foreach: facet =>
        facet match
          case link    : Facet.Link    => applyLink( link, target )
          case mention : Facet.Mention => applyMention( mention, target )
          case tag     : Facet.Tag     => applyTag( tag, target )
      new String(target.flatten, UTF8)
   
class BlueskyPost(
  record : CarBlock.Record,
  tid : String,
  did : String,
  tidToCid : Map[String,Cid],
  userHandleNoAt : Option[String],
  contentTransformer : String => String
) extends Post:
  def blobRefToSiteRooted( bref : BlobRef ) : Rooted =
    val suffix =
      bref.mimeType match
        case m if m.contains("jpeg") || m.contains("jpg") =>
          "jpeg"
        case m if m.contains("png") =>
          "png"
        case m if m.contains("webp") =>
          "webp"
        case _ =>
          "jpeg"
    BlueskyFossilphantSite.ImageDirSiteRooted.resolve( bref.cid.toMultibaseCidBase32 + "." + suffix )

  val id = record.cid.toMultibaseCidBase32
  val originalHost = "bsky.app"
  val user = userHandleNoAt.getOrElse(did)
  val userInMentionFormat = userHandleNoAt.fold(did)(uh => "@" + uh)
  val localId = id
  val displayNameOverride = None
  val url = s"https://bsky.app/profile/${did}/post/${tid}"
  val rawContent : String = record.text.getOrElse("(No content?!?)")
  val content : String = "<p>" + contentTransformer(BlueskyPost.applyFacets(rawContent,record.facets)) + "</p>"
  val published : Instant = record.createdAt
  val to : Seq[String] = Seq.empty // these are activitypub fields, it's unclear they have a bluesky analog
  val cc : Seq[String] = Seq.empty // these are activitypub fields, it's unclear they have a bluesky analog
  val sensitive : Boolean = false
  val followersUrl = s"https://bsky.app/profile/${did}/followers"
  val public : Boolean = true
  val followersVisible : Boolean = true

  def previewedLink : Option[Post.PreviewedLink] =
    def extractExternal( embed : Embed ) : Option[Embed.External] =
      embed match
        case external : Embed.External => Some(external)
        case Embed.RecordWithMedia( _, media ) => extractExternal( media )
        case _ => /* Not images! */
          None
    record.embed.flatMap( extractExternal ).flatMap: external =>
      Some( Post.PreviewedLink( external.uri, external.title, external.thumb.map(blobRefToSiteRooted), external.description ) )

  def images =
    def extractImages( embed : Embed ) : Seq[Post.Image] =
      embed match
        case images : Embed.Images =>
          images.images.map: imageRef =>
            val blobRef = imageRef.image
            Post.Image(blobRefToSiteRooted(blobRef),imageRef.alt)
        case Embed.RecordWithMedia( _, media ) =>
          extractImages( media )
        case _ => /* Not images! */
          Seq.empty
    record.embed.fold(Seq.empty)(e => extractImages(e) )

  val pollItems : immutable.Seq[Post.PollItem] = immutable.Seq.empty

  lazy val inReplyTo =
    record.reply match
      case Some( reply ) =>
        reply.parent.atUri match
          case AtUrlRegex( urlId, tpe, tid ) =>
            //println( s"urlId: $urlId   poster did: ${did}" )
            if urlId == did || Some(urlId) == userHandleNoAt then
              //println( "reply to self found." )
              InReplyTo.Self( tidToCid(tid).toMultibaseCidBase32 )
            else
              InReplyTo.Other( s"https://bsky.app/profile/${urlId}/post/${tid}" )
          case ick =>
            throw new InternalError(s"atUri '${ick}' failed to match its parsing regex ('${AtUrlRegex}')?!?")
      case None =>
        InReplyTo.NoOne

  def tags : Seq[String] = Seq.empty // tags are handled as facets in bsky
