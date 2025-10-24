package com.mchange.fossilphant.theme.tower

import unstatic.UrlPath.*
import com.mchange.fossilphant.*

val Tabs = ("Main", Rooted("/index.html")) :: ("Posts and Replies", Rooted("/postsWithReplies.html")) :: Nil

case class MainLayoutInput( siteRootedLocation : Rooted, title : String, tagline : String, topnav : String, content : String, bottomnav : String )

def threadOrPost(lpwc : LocatedPostWithContext, outLinkGen : Option[String => String] = None) : String =
  val post = lpwc.post
  val context = lpwc.context

  val skip =
    post.inReplyTo match
      case InReplyTo.NoOne | InReplyTo.Other(_) => false
      case InReplyTo.Self(_)                    => true

  if !skip then
    val thread =
      val tail = List.unfold(post) { post =>
        context.threadNexts
          .get(post.localId)
          .map(nextLid => (nextLid, context.publicPostsByLocalId(nextLid)))
      }
      post.localId :: tail
    if (thread.length > 1) then
      val posts = thread.map( context.publicPostsByLocalId )
      val raw = """<div class="thread">""" :: posts.map( p => post_html(lpwc.copy( post = p ), outLinkGen).text ) ::: "</div>" :: Nil
      raw.mkString("\n")
    else
      post_html(lpwc, outLinkGen).text
  else
    post_html(lpwc, outLinkGen).text



