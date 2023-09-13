package com.mchange.fossilphant

import unstatic.ztapir.ZTMain

object FossilphantSiteGenerator:
  class Runner( cfg : FossilphantConfig ) extends ZTMain(new FossilphantSite(cfg), "fossilphant-site")

  def main(args : Array[String]) : Unit =
    val runner = new Runner(config.MainFossilphantConfig)
    runner.main(args)

