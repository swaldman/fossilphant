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

case class UserHost( user : String, host : String ):
  override def toString() : String = s"${user}@${host}"

case class LocatedContext( siteRootedLocation : Rooted, context : FossilphantContext )

case class LocatedPostWithContext( siteRootedLocation : Rooted, post : Post, context : FossilphantContext )

case class LocatedPageWithContext( siteRootedLocation : Rooted, index : Int, pages : Seq[Iterable[Post]], context : FossilphantContext ):
  def page        : Int     = index + 1
  def isFirstPage : Boolean = index == 0
  def isLastPage  : Boolean = index == pages.size - 1
  def posts = pages(index)
  def numPages = pages.length

object ContentTransformer:
  def rehostTags( newHost : String ) : ContentTransformer = { content =>
    UnanchoredTagUrlRegex.replaceAllIn(content, m => s"""https://${newHost}/tags/${m.group(2)}""")
  }
type ContentTransformer = String => String