package com.mchange.fossilphant.bluesky

import com.mchange.fossilphant.*

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

object BlueskyFossilphantSite:
  val ImageDirSiteRooted = UrlPath.Rooted("/image")
class BlueskyFossilphantSite( config : FossilphantConfig, userHandleNoAt : Option[String], imageDir : os.Path ) extends AbstractFossilphantSite(config):

  // archiveLoc is from superclass
  val archiveCar = os.Path(archiveLoc, os.pwd)
  
  val bskyArchive = BskyArchive( archiveCar )

  lazy val context = FossilphantContext.forBluesky( bskyArchive, config, userHandleNoAt, imageDir )

  lazy val staticArchiveResources : ZTEndpointBinding.Source = new ZTEndpointBinding.Source:
    val imageDirEndpointBinding =
      ZTEndpointBinding.staticDirectoryServing( BlueskyFossilphantSite.this.location(BlueskyFossilphantSite.ImageDirSiteRooted), imageDir.toNIO, immutable.Set("imageDir") )

    val avatarBinding = 
      val path =
        val avatarFiles = os.list( imageDir ).filter( _.lastOpt.get.startsWith( "avatar" ) ) // ugly .get, but we should find root
        avatarFiles.length match
          case 0 => throw new MissingAvatar("No file like 'avatar.<suffix>' found among downloaded images!")
          case 1 => avatarFiles.head
          case n => 
            val matches = avatarFiles.map( _.lastOpt.get ).mkString(", ")
            throw new MissingAvatar(s"MULTIPLE ($n) files among downloaded images are like 'avatar.<suffix>'! Can't discern which of ${matches} to use.")
      val avatarFileName = path.lastOpt.get
      ZTEndpointBinding.staticFileServing( BlueskyFossilphantSite.this.location(avatarFileName), path.toNIO, immutable.Set("avatar", avatarFileName) )


    val endpointBindings = imageDirEndpointBinding :: avatarBinding :: Nil

