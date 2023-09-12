package com.mchange.fossilphant

import java.time.Instant
import scala.collection.*
import scala.util.{Failure, Success, Try}
import unstatic.UrlPath.*

object Post:
  object Image:
    val SupportedTypes = immutable.Set("image/jpeg","image/gif","image/png")
  case class Image(siteRootedPath : Rooted, alt : Option[String] )
  given ReverseChronologicalPublished : Ordering[Post] = Ordering.by[Post,Instant]( _.published ).reverse
class Post( createActivity : UjsonObjValue):
  val (originalHost, user, localId ) =
    createActivity("id").str match
      case ActivityUrlRegex(oh, u, lid) => (oh, u, lid)
      case other => throw new UnexpectedValueFormat(s"Activity id '${other}' not in expected format.")
  def content : String = createActivity("object").obj("content").str
  def published : Instant =
    val timestamp = createActivity("published").str
    val ta = MastodonDateTimeFormatter.parse(timestamp)
    Instant.from(ta)
  def to : Seq[String] = createActivity("to").arr.map( _.str ).toSeq
  def cc : Seq[String] = createActivity("cc").arr.map( _.str ).toSeq
  def sensitive : Boolean = createActivity("object").obj("sensitive").str.toBoolean
  def followersUrl = s"https://${originalHost}/users/${user}/followers"
  def allRecipients : Seq[String] = to ++ cc
  def public : Boolean = allRecipients.contains(PublicId)
  def followersVisible : Boolean = allRecipients.contains(followersUrl)
  def images =
    val attachments = createActivity("object").obj("attachment").arr.map( _.obj )
    def isImage( jso : UjsonObjValue) =
      jso("type").str == "Document" && Post.Image.SupportedTypes(jso("mediaType").str)
    attachments.filter(isImage).map { jso =>
      val path = Rooted(jso("url").str)
      val alt =
        val raw = jso("name")
        if raw.isNull then None else Some(raw.str)
      Post.Image(path,alt)
    }

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
end Post
