package com.mchange.fossilphant

import com.mchange.fossilphant.Post.PollItem

import java.time.Instant
import scala.collection.*
import scala.util.{Failure, Success, Try}
import unstatic.UrlPath.*

import com.mchange.bskyarchive.CarBlock

object BlueskyPost:
  given ReverseChronologicalPublished : Ordering[BlueskyPost] with
    def compare( x : BlueskyPost, y : BlueskyPost ) : Int = Post.ReverseChronologicalPublished.compare( x, y )
class BlueskyPost( record : CarBlock.Record, tid : String, did : String, userHandleNoAt : Option[String], contentTransformer : String => String ) extends Post:
  val id = record.cid.toMultibaseCidBase32
  val originalHost = "bsky.app"
  val user = userHandleNoAt.getOrElse(did)
  val userInMentionFormat = userHandleNoAt.fold(did)(uh => "@" + uh)
  val localId = id
  val displayNameOverride = None
  val url = s"https://bsky.app/profile/${did}/post/${tid}"
  val rawContent : String = record.text.getOrElse("(No content?!?)")
  val content : String = contentTransformer(rawContent)
  val published : Instant = record.createdAt
  val to : Seq[String] = Seq.empty // FIXME
  val cc : Seq[String] = Seq.empty // FIXME
  val sensitive : Boolean = false
  val followersUrl = s"https://bsky.app/profile/${did}/followers"
  val public : Boolean = true
  val followersVisible : Boolean = true
  def images = Seq.empty // FIXME
  /*
    val attachments = createActivity("object").obj("attachment").arr.map( _.obj )
    def isImage( jso : UjsonObjValue) =
      jso("type").str == "Document" && Post.Image.SupportedTypes(jso("mediaType").str)
    attachments.filter(isImage).map { jso =>
      // grrr... some instances prepend an instancename before /media_attachments,
      // but in the archive, the media is in a root-level /media_attachments
      // this feels very hackish -- wrong even! what if an archive legit chose a path
      // that contains /media_attachments in the actual archive?
      //
      // for now it seems to work. but i may have to get in the business of checking the
      // archive and then configuring the parse
      val path =
        val raw = jso("url").str
        val realStart = raw.indexOf("/media_attachments")
        val fixed = if realStart > 0 then raw.substring( realStart ) else raw
        Rooted( fixed )
      val alt =
        val raw = jso("name")
        if raw.isNull then None else Some(raw.str)
      Post.Image(path,alt)
    }
  */  
  val pollItems : immutable.Seq[Post.PollItem] = immutable.Seq.empty

  lazy val inReplyTo = InReplyTo.NoOne // FIXME
/*  
    val irt = createActivity("object").obj("inReplyTo")
    if irt.isNull then
      InReplyTo.NoOne
    else
      irt.str match
        case null => InReplyTo.NoOne
        case StatusUrlRegex(h, u, lid) if h == originalHost && u == user => InReplyTo.Self(lid)
        case mbu if mbu.startsWith("http") => InReplyTo.Other(mbu) //XXX: Not the greatest URL validation
        case whatev => throw new UnexpectedValueFormat(s"Status id '${whatev}' not in expected format.")
 */

  def tags : Seq[String] = Seq.empty // FIXME


