package config

import java.time.ZoneId
import com.mchange.fossilphant.*

/*
  All values have defaults defined (except archivePath if environment variable FOSSILPHANT_ARCHIVE is not set).

  But you can configure:

    archivePath
    mainTitle
    mainTagline
    overrideDisplayName
    newSelfUrl
    themeName
    themeConfig
    pageLength
    contentTransformer
    toFollowersAsPublic
    sensitiveAsPublic
    suppressLinksToOriginal
    timestampTimezone

  See detailed explanations below!
 */

val MainFossilphantConfig = FossilphantConfig(
  // e.g. Some( "/path/to/my/archive-dir" ) or Some( "/path/to/my/archive-dir.tar.gz" ) or Some( "/path/to/my/archive-dir.tgz" )
  //      optional and overridden by environment variable FOSSILPHANT_ARCHIVE, if present
  archivePath = None,

  // e.g. Some( "Steve's fosstodon archive" )
  //      optional, a default title will be generated from the archive
  mainTitle = None,

  // e.g. Some( "My terrible toots" )
  //      optional, a default tagline will be generated if none is provided
  mainTagline = None,

  // e.g. Some( "Steve Poophead" )
  //      optional, overrides user's name discovered from archive if present
  overrideDisplayName = None,

  // e.g. Some( "https://mynewhome.social/@steve" )
  //      optional, redirects links to self in post to a new identity
  newSelfUrl = None,

  // e.g. "shatter" or "tower" or "coolnewtheme"
  //       defaults to "shatter" if omitted
  //       currently only "shatter" and "tower" are defined,
  //       but maybe people will add new themes!
  themeName = "shatter",

  // e.g. Map( "page.background.color" -> "#CCCCFF" )
  //      theme-dependent, but keys and defaults of the current theme
  //      are shown, commented out, below
  themeConfig = Map(
    //"page.background.color" -> "rgb(225,225,225)",
    //"post.background.color" -> "#FFFFFF",
    //"post.text.color" -> "black",
    //"outer.text.color" -> "black",
    //"outer.link.color" -> "#0000EE",
    //"outer.link.color.visited" -> "#551A8B",
    //"post.link.color" -> "#0000EE",
    //"post.link.color.visited" -> "#551A8B",
    //"post.border.color" -> "gray",
    //"thread.border.color" -> "black",
  ),

  // e.g. 50
  //      defaults to 20 if omitted
  //      for themes that support paging,
  //      how many posts should appear on each page?
  pageLength = 20,

  // e.g. ContentTransformer.rehostTags( "mastodon.social" )
  //      transforms post content while generating.
  //      for example, if your instance is disappearing,
  //      tags would normally become dead links.
  //      if you set this to ContentTransformer.rehostTags( "mastodon.social" ),
  //      then tag links will go to the tag's index on https://mastodon.social/
  contentTransformer = identity,

  // e.g. true
  //      defaults to false if omitted
  //      if true, posts posted as followers-only will be
  //      included as public posts on the generated site
  //      OTHERWISE THEY WILL BE EXCLUDED
  //
  //     (posts directed neither to the public nor to followers
  //      are always excluded)
  toFollowersAsPublic = false,

  // e.g. true
  //      defaults to false if omitted
  //      if true, posts marked as sensitive will be
  //      included as public posts on the generated site
  //      OTHERWISE THEY WILL BE EXCLUDED
  //
  //      barriers and content warnings are not yet supported
  //      if made public, sensitive posts will be openly published
  sensitiveAsPublic = false,

  // e.g. true
  //      defaults to false if omitted
  //      if true, themes that might otherwise try to link back to
  //      the original, "live" posts should avoid doing so.
  //      the intention is to minimize broken links from archives
  //      of defunct instances
  suppressLinksToOriginal = false,

  // e.g. ZoneId.of("America/New_York")
  //      defaults to ZoneId.systemDefault() if omitted
  //      when timestamps are printed into posts,
  //      the date of a moment in time depends upon
  //      in what timezone it is interpreted as occurring
  //      you can explicitly set this of you want
  timestampTimezone = ZoneId.systemDefault(),
)