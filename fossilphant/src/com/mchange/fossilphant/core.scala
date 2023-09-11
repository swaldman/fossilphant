package com.mchange.fossilphant

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.{DateTimeFormatter, FormatStyle}
import scala.collection.*
import scala.util.{Failure, Success, Try}
import unstatic.UrlPath.*

type UjsonObjValue = upickle.core.LinkedHashMap[String,ujson.Value]

val MastodonDateTimeFormatter = DateTimeFormatter.ISO_INSTANT

val TimestampDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
val TimestampTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

def formatShortDate( zdt : ZonedDateTime) : String =
  //DateTimeFormatter.ISO_LOCAL_DATE.format(zdt)
  TimestampDateFormatter.format(zdt)

def formatShortTime( zdt : ZonedDateTime) : String =
  //DateTimeFormatter.ISO_LOCAL_TIME.format(zdt)
  TimestampTimeFormatter.format( zdt )

val ReverseChronologicalPublished : Ordering[UjsonObjValue] =
  val forward =
    Ordering.by[UjsonObjValue,Instant] { jso =>
      val attempt = Try {
        val timestamp = jso("published").str
        val ta = MastodonDateTimeFormatter.parse(timestamp)
        Instant.from(ta)
      }
      attempt match
        case Success(i) => i
        case Failure(t) =>
          System.err.println("Assigning random early time, error reading publication date from " + jso)
          t.printStackTrace()
          Instant.ofEpochMilli(math.round(math.random * 1_000_000).toLong)
    }
  forward.reverse

def afterLastSlash( s : String ) : String =
  val lastSlash = s.lastIndexOf('/')
  if lastSlash >= 0 then
    s.substring(lastSlash + 1)
  else
    throw new UnexpectedValueFormat(s"Expected at least one slash ('/') in '${s}', none found.")

val ActivityUrlRegex = """^https\:\/\/([^\/]+)\/users\/([^\/]+)\/statuses\/([^\/]+)\/activity$""".r
val StatusUrlRegex = """^https\:\/\/([^\/]+)\/users\/([^\/]+)\/statuses\/([^\/]+)$""".r

val PublicId = "https://www.w3.org/ns/activitystreams#Public"

def effectivelyPublic( config : FossilphantConfig )( post : Post ) : Boolean =
  val sensitiveOk =
    if !config.sensitiveAsPublic then !post.sensitive else true

  val receipientsOk =
    if config.toFollowersAsPublic then
      post.public || post.followersVisible
    else
      post.public

  sensitiveOk && receipientsOk

object InReplyTo:
  case object NoOne extends InReplyTo
  case class Self( statusId : String ) extends InReplyTo
  case class Other( url : String ) extends InReplyTo
sealed trait InReplyTo

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

  def nameForDisplay( userDisplayName : Option[String]) = userDisplayName.getOrElse(user)
end Post

case class FossilphantContext(
  config : FossilphantConfig,
  reverseChronologicalPublicPosts : Seq[Post],
  publicPostsByLocalId : Map[String,Post],
  threadNexts : Map[String,String],
  rawOutbox : UjsonObjValue
)

case class LocatedContext( siteRootedLocation : Rooted, context : FossilphantContext )

object LocatedPostWithContext:
  def apply(post : Post, locatedContext : LocatedContext) : LocatedPostWithContext =
    LocatedPostWithContext(locatedContext.siteRootedLocation, post, locatedContext.context)
case class LocatedPostWithContext( siteRootedLocation : Rooted, post : Post, context : FossilphantContext ):
  def userDisplayName = this.post.nameForDisplay( this.context.config.userDisplayName )

case class FossilphantConfig(
  userDisplayName     : Option[String],
  newTagHost          : Option[String],
  newSelfHost         : Option[String],
  toFollowersAsPublic : Boolean = false,
  sensitiveAsPublic   : Boolean = false,
  timestampTimezone   : ZoneId = ZoneId.systemDefault()
)



