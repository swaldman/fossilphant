package com.mchange.fossilphant

import scala.collection.*

import unstatic.*
import unstatic.ztapir.*
import unstatic.ztapir.simple.*

import unstatic.*, UrlPath.*

import java.nio.file.Path as JPath
import java.lang.System // otherwise shadowed by zio.*

import untemplate.Untemplate.AnyUntemplate

import com.mchange.bskyarchive.BskyArchive

import zio.*

class BlueskyFossilphantSite( config : FossilphantConfig, userHandleNoAt : Option[String], imageDir : os.Path ) extends AbstractFossilphantSite(config):

  // archiveLoc is from superclass
  val archiveCar = os.Path(archiveLoc, os.pwd)
  
  val bskyArchive = BskyArchive( archiveCar )

  lazy val context = FossilphantContext.forBluesky( bskyArchive, config, userHandleNoAt, imageDir )

  lazy val staticArchiveResources : ZTEndpointBinding.Source = new ZTEndpointBinding.Source:
    val imageDirEndpointBinding =
      ZTEndpointBinding.staticDirectoryServing( BlueskyFossilphantSite.this.location("image"), imageDir.toNIO, immutable.Set("imageDir") )

    val endpointBindings = imageDirEndpointBinding :: Nil

