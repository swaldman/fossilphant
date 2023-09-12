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



