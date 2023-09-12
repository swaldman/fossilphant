package com.mchange.fossilphant

import unstatic.ztapir.ZTMain

object FossilphantSiteGenerator:
  class Runner( cfg : FossilphantConfig ) extends ZTMain(new FossilphantSite(cfg), "fossilphant-site")

  val config =
    import java.time.ZoneId
    FossilphantConfig (
      mainTagline = Some( "I guess it's a Mastodon archive!" ),
      overrideDisplayName = None,
      newSelfUrl = Some( "https://econtwitter.net/@interfluidity" ),
      toFollowersAsPublic = true,
      sensitiveAsPublic = true,
      timestampTimezone = ZoneId.of("America/New_York")
    )

  def main(args : Array[String]) : Unit =
    val runner = new Runner(config)
    runner.main(args)


