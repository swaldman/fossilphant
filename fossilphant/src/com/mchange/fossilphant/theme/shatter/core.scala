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

def tab( contents : String, destLocation : Rooted, currentLocation : Rooted, onLocation : (Rooted,Rooted) => Boolean) : String =
  if ( onLocation( destLocation, currentLocation ) ) then
    s"""<span class="tab current">${contents}</span>"""
  else
    val href=currentLocation.relativizeSibling(destLocation)
    s"""<span class="tab"><a href="${href}">${contents}</a></span>"""

val FirstMainLoc = Rooted("/main_1.html")
val FirstWithrepliesLoc = Rooted("/withreplies_1.html")

val OnMain : (Rooted, Rooted) => Boolean = (_,current) => current.elements.last.startsWith("main_")
val OnWithreplies : (Rooted, Rooted) => Boolean = (_,current) => current.elements.last.startsWith("withreplies_")
