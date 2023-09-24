package com.mchange.fossilphant

import unstatic.ztapir.ZTMain

object FossilphantSiteGenerator:
  class Runner( cfg : FossilphantConfig ) extends ZTMain(new FossilphantSite(cfg), "fossilphant-site")

  def main(args : Array[String]) : Unit =
    val config =
      var raw = _root_.config.MainFossilphantConfig
      sys.env.get(Env.Archive).foreach { envArchivePath =>
        raw.archivePath.foreach { _ =>
          System.err.println(s"""Overriding configured archive path with environment variable ${Env.Archive}="${envArchivePath}"""")
        }
        raw = raw.copy( archivePath = Some(envArchivePath) )
      }
      sys.env.get(Env.Theme).foreach { envThemeName =>
        System.err.println(s"""Overriding configured theme name with environment variable ${Env.Theme}="${envThemeName}"""")
        raw = raw.copy( themeName = envThemeName )
      }
      raw
    val runner = new Runner(config)
    runner.main(args)

