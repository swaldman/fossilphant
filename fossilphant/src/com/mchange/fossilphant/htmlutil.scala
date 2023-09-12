package com.mchange.fossilphant

def threadOrPost(lpwc : LocatedPostWithContext, showInReplyTo : Boolean = false) : String =
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
      val raw = """<div class="thread">""" :: posts.map( p => post_html(lpwc.copy( post = p ), showInReplyTo).text ) ::: "</div>" :: Nil
      raw.mkString("\n")
    else
      post_html(lpwc, showInReplyTo).text
  else
    post_html(lpwc, showInReplyTo).text

