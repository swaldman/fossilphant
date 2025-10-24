package com.mchange.fossilphant

import com.mchange.fossilphant.Post.PollItem

import java.time.Instant
import scala.collection.*
import scala.util.{Failure, Success, Try}
import unstatic.UrlPath.*

object MastodonPost:
  given ReverseChronologicalPublished : Ordering[MastodonPost] with
    def compare( x : MastodonPost, y : MastodonPost ) : Int = Post.ReverseChronologicalPublished.compare( x, y )
class MastodonPost( createActivity : UjsonObjValue, contentTransformer : String => String ) extends Post:
  val id = createActivity("id").str
  val (originalHost, user, localId ) =
    id match
      case ActivityUrlRegex(oh, u, lid) => (oh, u, lid)
      case other => throw new UnexpectedValueFormat(s"Activity id '${other}' not in expected format.")
  def url = createActivity("object").obj("url").str
  def rawContent : String = createActivity("object").obj("content").str
  def content : String = contentTransformer(rawContent)
  def published : Instant =
    val timestamp = createActivity("published").str
    val ta = MastodonDateTimeFormatter.parse(timestamp)
    Instant.from(ta)
  def to : Seq[String] = createActivity("to").arr.map( _.str ).toSeq
  def cc : Seq[String] = createActivity("cc").arr.map( _.str ).toSeq
  def sensitive : Boolean = createActivity("object").obj("sensitive").bool
  def followersUrl = s"https://${originalHost}/users/${user}/followers"
  def public : Boolean = allRecipients.contains(PublicId)
  def followersVisible : Boolean = allRecipients.contains(followersUrl)
  def images =
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
  def pollItems : immutable.Seq[Post.PollItem] =
    val isPoll = createActivity("object").obj("type").str == "Question"
    if isPoll then
      createActivity("object").obj("oneOf").arr.map { jso =>
        val dict = jso.obj
        val text = dict("name").str
        val count = dict("replies").obj("totalItems").num.toInt
        PollItem( text, count )
      }.toList
    else
      Nil

  lazy val inReplyTo =
    val irt = createActivity("object").obj("inReplyTo")
    if irt.isNull then
      InReplyTo.NoOne
    else
      irt.str match
        case null => InReplyTo.NoOne
        case StatusUrlRegex(h, u, lid) if h == originalHost && u == user => InReplyTo.Self(lid)
        case mbu if mbu.startsWith("http") => InReplyTo.Other(mbu) //XXX: Not the greatest URL validation
        case whatev => throw new UnexpectedValueFormat(s"Status id '${whatev}' not in expected format.")

  def tags : Seq[String] = createActivity("object").obj("tag").arr.map( _.str ).toSeq
