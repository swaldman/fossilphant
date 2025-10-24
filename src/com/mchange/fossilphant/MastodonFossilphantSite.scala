package com.mchange.fossilphant

import scala.collection.*

import unstatic.*
import unstatic.ztapir.*
import unstatic.ztapir.simple.*

import unstatic.*, UrlPath.*

import java.nio.file.Path as JPath
import java.lang.System // otherwise shadowed by zio.*

import untemplate.Untemplate.AnyUntemplate

import zio.*

class MastodonFossilphantSite( config : FossilphantConfig ) extends AbstractFossilphantSite(config):

  lazy val archiveDir : os.Path =
    import org.rauschig.jarchivelib.{ArchiveFormat,ArchiverFactory}
    val rawPath = os.Path(archiveLoc, os.pwd)
    if !os.exists(rawPath) then
      throw new BadArchivePath( s"No file or directory exists at specified path '${rawPath}'." )
    else if os.isDir(rawPath) then
      rawPath
    else if archiveLoc.endsWith(".tgz") || archiveLoc.endsWith(".tar.gz") then
      val tmpDir = os.temp.dir()
      val archiver = ArchiverFactory.createArchiver("tar", "gz")
      System.err.println(s"Extracting tgz archive '${rawPath}' into '${tmpDir}'.")
      archiver.extract(rawPath.toIO, tmpDir.toIO)
      tmpDir
    else if archiveLoc.endsWith(".zip") then
      val tmpDir = os.temp.dir()
      val archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP)
      System.err.println(s"Extracting zip archive '${rawPath}' into '${tmpDir}'.")
      archiver.extract(rawPath.toIO, tmpDir.toIO)
      tmpDir
    else
      throw new BadArchivePath( s"Archive location '${archiveLoc}' is neither a directory nor a file ending in '.tgz' or '.tar.gz' as expected." )

  lazy val context = FossilphantContext.forMastodon( archiveDir, config )

  lazy val staticArchiveResources : ZTEndpointBinding.Source = new ZTEndpointBinding.Source:
    val mediaAttachmentsDirName = "media_attachments"
    val mediaAttachmentsPath = archiveDir / mediaAttachmentsDirName

    val avatarFileName = "avatar.jpg"
    val avatarPath = archiveDir / avatarFileName

    val mediaAttachmentsEndpointBinding =
      ZTEndpointBinding.staticDirectoryServing( MastodonFossilphantSite.this.location(mediaAttachmentsDirName), mediaAttachmentsPath.toNIO, immutable.Set(mediaAttachmentsDirName) )
    val avatarStaticBinding =
      ZTEndpointBinding.staticFileServing( MastodonFossilphantSite.this.location(avatarFileName), avatarPath.toNIO, immutable.Set("avatar", avatarFileName) )
    val endpointBindings = mediaAttachmentsEndpointBinding :: avatarStaticBinding :: Nil

