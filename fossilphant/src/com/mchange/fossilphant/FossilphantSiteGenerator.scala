package com.mchange.fossilphant

import unstatic.ztapir.ZTMain

object FossilphantSiteGenerator:
  class Runner( cfg : FossilphantConfig ) extends ZTMain(new FossilphantSite(cfg), "fossilphant-site")

  val config =
    import java.time.ZoneId
    FossilphantConfig (
      //themeName = "tower",
      themeName = "shatter",
      //mainTagline = Some( "I guess it's a Mastodon archive!" ),
      overrideDisplayName = None,
      newSelfUrl = Some( "https://econtwitter.net/@interfluidity" ),
      contentTransformer = ContentTransformer.rehostTags("mastodon.social"),
      toFollowersAsPublic = true,
      sensitiveAsPublic = true,
      timestampTimezone = ZoneId.of("America/New_York")
    )

  def main(args : Array[String]) : Unit =
    val runner = new Runner(config)
    runner.main(args)

