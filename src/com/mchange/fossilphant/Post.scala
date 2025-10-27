package com.mchange.fossilphant

import com.mchange.fossilphant.Post.PollItem

import java.time.Instant
import scala.collection.*
import scala.util.{Failure, Success, Try}
import unstatic.UrlPath.*

object Post:
  object Image:
    val SupportedTypes = immutable.Set("image/jpeg","image/gif","image/png")
  case class PreviewedLink( href : String, title : String, thumbnailSiteRootedPath : Option[Rooted], description : Option[String] )
  case class Image(siteRootedPath : Rooted, alt : Option[String] )
  case class PollItem( text : String, count : Int )
  given ReverseChronologicalPublished : Ordering[Post] = Ordering.by[Post,Instant]( _.published ).reverse
trait Post:
  def id : String
  def originalHost : String
  def user : String
  def userInMentionFormat : String
  def displayNameOverride : Option[String]
  def localId : String
  def url : String
  def rawContent : String
  def content : String
  def published : Instant
  def to : Seq[String]
  def cc : Seq[String]
  def sensitive : Boolean
  def followersUrl : String
  def public : Boolean
  def followersVisible : Boolean
  def previewedLink : Option[Post.PreviewedLink]
  def images : Seq[Post.Image]
  def pollItems : immutable.Seq[Post.PollItem]
  def inReplyTo : InReplyTo
  def tags : Seq[String]
  def quotedPostHtml : Option[String]

  def allRecipients : Seq[String] = to ++ cc
end Post
