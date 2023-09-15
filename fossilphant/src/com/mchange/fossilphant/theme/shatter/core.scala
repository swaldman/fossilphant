package com.mchange.fossilphant.theme.shatter

import com.mchange.fossilphant.{LocatedPageWithContext, theme}
import unstatic.PageBase
import unstatic.UrlPath.*

val image_html = theme.tower.image_html
val layout_main_html = theme.tower.layout_main_html
val post_html = theme.tower.post_html

val threadOrPost = theme.tower.threadOrPost

val MainLayoutInput  = theme.tower.MainLayoutInput
type MainLayoutInput = theme.tower.MainLayoutInput

def postOutLinkGen( localId : String ) : String = s"single_${localId}.html"

def repage( pathStr : String, newPageNum : Int ) : String =
  pathStr.substring(0,pathStr.lastIndexOf('_')+1) + newPageNum + ".html"

def firstLink( contents : String, lpwc : LocatedPageWithContext ) =
  if lpwc.isFirstPage then
    s"""<span class="disabled">${contents}</span>"""
  else
    val loc = lpwc.siteRootedLocation
    val currentSiteRootedPath = loc.toString
    val prevSiteRootedPath = repage(currentSiteRootedPath, 1)
    val prevRelPath = loc.relativizeSibling( Rooted(prevSiteRootedPath) )
    s"""<a href="${prevRelPath}">${contents}</a>"""


def prevLink( contents : String, lpwc : LocatedPageWithContext ) =
  if lpwc.isFirstPage then
    s"""<span class="disabled">${contents}</span>"""
  else
    val loc = lpwc.siteRootedLocation
    val currentSiteRootedPath = loc.toString
    val prevSiteRootedPath = repage(currentSiteRootedPath, lpwc.page-1)
    val prevRelPath = loc.relativizeSibling( Rooted(prevSiteRootedPath) )
    s"""<a href="${prevRelPath}">${contents}</a>"""

def nextLink(contents: String, lpwc: LocatedPageWithContext) =
  if lpwc.isLastPage then
    s"""<span class="disabled">${contents}</span>"""
  else
    val loc = lpwc.siteRootedLocation
    val currentSiteRootedPath = loc.toString
    val nextSiteRootedPath = repage(currentSiteRootedPath, lpwc.page + 1)
    val nextRelPath = loc.relativizeSibling(Rooted(nextSiteRootedPath))
    s"""<a href="${nextRelPath}">${contents}</a>"""

def lastLink( contents : String, lpwc : LocatedPageWithContext ) =
  if lpwc.isLastPage then
    s"""<span class="disabled">${contents}</span>"""
  else
    val loc = lpwc.siteRootedLocation
    val currentSiteRootedPath = loc.toString
    val prevSiteRootedPath = repage(currentSiteRootedPath, lpwc.numPages)
    val prevRelPath = loc.relativizeSibling( Rooted(prevSiteRootedPath) )
    s"""<a href="${prevRelPath}">${contents}</a>"""

// this is inelegant, but simple, easy to follow
// i'm tired of whiffing more elegant logic split between here and the stylesheets

val TabsMainWide =
  s"""<span class="tab current">Main</span> <span class="disc">•</span> <span class="tab"><a href="withreplies_1.html">Posts and replies</a></span>"""
val TabsMainNarrow =
  s"""Switch to: <span class="tab"><a href="withreplies_1.html">Posts and replies</a></span>"""
val TabsRepliesWide =
  s"""<span class="tab"><a href="main_1.html">Main</a></span> <span class="disc">•</span> <span class="tab current">Posts and replies</span>"""
val TabsRepliesNarrow =
  s"""Switch to: <span class="tab"><a href="main_1.html">Main</a></span>"""
val TabsSingleWide =
  s"""<span class="tab"><a href="main_1.html">Main</a></span> <span class="disc">•</span> <span class="tab"><a href="withreplies_1.html">Posts and replies</a></span>"""
val TabsSingleNarrow = TabsSingleWide

def wideNarrow( tabsWide : String, tabsNarrow : String ) =
  s"""<div class="tabs-wide">${tabsWide}</div><div class="tabs-narrow">${tabsNarrow}</div>"""

val TabsMain = wideNarrow( TabsMainWide, TabsMainNarrow )
val TabsReplies = wideNarrow( TabsRepliesWide, TabsRepliesNarrow )
val TabsSingle = wideNarrow( TabsSingleWide, TabsSingleNarrow )

def tabsForLocation( location : Rooted ) : String =
  if location.elements.last.startsWith("main") then
    TabsMain
  else if location.elements.last.startsWith("withreplies") then
    TabsReplies
  else
    TabsSingle
end tabsForLocation

