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

class FossilphantSite( val config : FossilphantConfig ) extends ZTSite.SingleRootComposite( JPath.of("fossilphant/static") ):

  // these are... not so good. But this site should produce only
  // relative paths, so it should not matter
  override val serverUrl : Abs    = Abs("https://www.unknown.com/")
  override val basePath  : Rooted = Rooted.root

  // make this a command line arg soon!
  val archiveLoc : String =
    (sys.env.get("FOSSILPHANT_ARCHIVE") orElse config.archivePath)
      .getOrElse( throw new BadArchivePath("Location of Mastodon archive is defined neither in MASTODON_ARCHIVE environment variable nor in config.") )

  lazy val archiveDir : os.Path =
    import org.rauschig.jarchivelib.ArchiverFactory
    val rawPath = os.Path(archiveLoc)
    if !os.exists(rawPath) then
      throw new BadArchivePath( s"No file or directory exists at specified path '${rawPath}'." )
    else if os.isDir(rawPath) then
      rawPath
    else if archiveLoc.endsWith(".tgz") || archiveLoc.endsWith(".tar.gz") then
      val tmpDir = os.temp.dir()
      val archiver = ArchiverFactory.createArchiver("tar", "gz")
      System.err.println(s"Extracting archive '${rawPath}' into '${tmpDir}'.")
      archiver.extract(rawPath.toIO, tmpDir.toIO)
      tmpDir
    else
      throw new BadArchivePath( s"Archive location '${archiveLoc}' is neither a directory nor a file ending in '.tgz' or '.tar.gz' as expected." )

  lazy val context = FossilphantContext( archiveDir, config )

  val HtmlRegex = """^(?:.*?)([^\.]*)_html_gen$""".r
  val CssRegex  = """^(?:.*?)([^\.]*)_css_gen""".r

  // no subdirectories! we generate theme resources into a single directory
  object GenUntemplates extends ZTEndpointBinding.Source:
    val themeIndex = IndexedUntemplates
    val genUntemplates = themeIndex.filter( (k,_) => k.startsWith("com.mchange.fossilphant.theme." + config.themeName) && k.endsWith("_gen") )

    val typedGenUntemplates = genUntemplates.collect {
      case tup : (String, untemplate.Untemplate[LocatedContext,Nothing]) => tup
    }

    if genUntemplates.size != typedGenUntemplates.size then
      val badlyTypedGenUntemplates = genUntemplates.keySet.removedAll(typedGenUntemplates.keySet)
      scribe.warn(s"""The following theme untemplates were badly types and will be ignored: ${badlyTypedGenUntemplates.mkString(", ")}""")

    def endpointBindingForGenUntemplate( tup : (String, untemplate.Untemplate[LocatedContext,Nothing]) ) : ZTEndpointBinding =
      tup(0) match
        case HtmlRegex(baseName) =>
          val location = Rooted(s"/${baseName}.html")
          val task = ZIO.attempt {
            tup(1)(LocatedContext(location,context)).text
          }
          FossilphantSite.this.publicReadOnlyHtml( location, task, None, immutable.Set(baseName,s"${baseName}.html"), true, false )
        case CssRegex(baseName) =>
          val location = Rooted(s"/${baseName}.css")
          val task = ZIO.attempt {
            tup(1)(LocatedContext(location,context)).text
          }
          ZTEndpointBinding.publicReadOnlyCss( location, FossilphantSite.this, task, None, immutable.Set(baseName,s"${baseName}.css") )
        case other =>
          throw new BadThemeUntemplate(s"'${other}' appears to be a theme untemplate that would generate an unknown or unexpected file type.")

    val endpointBindings = typedGenUntemplates.map( endpointBindingForGenUntemplate ).toSeq
  end GenUntemplates

  object StaticArchiveResources extends ZTEndpointBinding.Source:
    val mediaAttachmentsDirName = "media_attachments"
    val mediaAttachmentsPath = archiveDir / mediaAttachmentsDirName

    val avatarFileName = "avatar.jpg"
    val avatarPath = archiveDir / avatarFileName

    val mediaAttachmentsEndpointBinding =
      ZTEndpointBinding.staticDirectoryServing( FossilphantSite.this.location(mediaAttachmentsDirName), mediaAttachmentsPath.toNIO, immutable.Set(mediaAttachmentsDirName) )
    val avatarStaticBinding =
      ZTEndpointBinding.staticFileServing( FossilphantSite.this.location(avatarFileName), avatarPath.toNIO, immutable.Set("avatar", avatarFileName) )
    val endpointBindings = mediaAttachmentsEndpointBinding :: avatarStaticBinding :: Nil
  end StaticArchiveResources

  // avoid conflicts, but early items in the lists take precedence over later items
  override val endpointBindingSources : immutable.Seq[ZTEndpointBinding.Source] = immutable.Seq( StaticArchiveResources, GenUntemplates )

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

