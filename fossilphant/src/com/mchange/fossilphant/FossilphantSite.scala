package com.mchange.fossilphant

import scala.collection.*

import unstatic.*
import unstatic.ztapir.*
import unstatic.ztapir.simple.*

import unstatic.*, UrlPath.*

import java.nio.file.Path as JPath

import untemplate.Untemplate.AnyUntemplate

import zio.*

class FossilphantSite( val config : FossilphantConfig ) extends ZTSite.SingleRootComposite( JPath.of("fossilphant/static") ):

  // edit this to where your site will actually be served!
  override val serverUrl : Abs    = Abs("https://www.example.com/")
  override val basePath  : Rooted = Rooted.root

  // make this a command line arg soon!
  lazy val archiveLoc : String = sys.env("FOSSILPHANT_ARCHIVE")

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
      archiver.extract(rawPath.toIO, tmpDir.toIO)
      tmpDir
    else
      throw new BadArchivePath( s"Archive location '${archiveLoc}' is neither a directory nor a file ending in '.tgz' or '.tar.gz' as expected." )

  lazy val outboxJsonPath = archiveDir / "outbox.json"

  // FIXME: we are very brittle-ly assuming everything goes right...
  val outbox = ujson.read(os.read.stream(outboxJsonPath) )
  val items = outbox.obj("orderedItems").arr.map( _.obj )
  val grouped = items.groupBy( _("type").str ) // expect "Announce", "Create"
  val unsortedPostJsons = grouped("Create")

  val publicPosts =
    unsortedPostJsons
      .map( postJson => new Post(postJson) )
      .filter( effectivelyPublic(config) )

  val publicPostsByLocalId = publicPosts.map( post => (post.localId, post)).toMap

  val threadNexts =
    publicPosts.view
      .map( post => (post.localId, post.inReplyTo)  )
      .collect { case (lid, InReplyTo.Self(prev)) => (prev,lid)}
      .toMap

  val reverseChronologicalPublicPosts =
    immutable.SortedSet.from( publicPosts ).toSeq

  val context = FossilphantContext( config, reverseChronologicalPublicPosts, publicPostsByLocalId, threadNexts, outbox.obj )
  // customize this to what the layout you want requires!
  case class MainLayoutInput( renderLocation : SiteLocation, mbTitle : Option[String], mainContentHtml : String )

  val HtmlRegex = """^(?:.*?)([^\.]*)_html_gen$""".r
  val CssRegex  = """^(?:.*?)([^\.]*)_css_gen""".r

  // no subdirectories! we generate theme resources into a single directory
  object GenUntemplates extends ZTEndpointBinding.Source:
    val themeIndex = IndexedUntemplates
    val genUntemplates = themeIndex.filter( (k,_) => k.endsWith("_gen") )

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
      userDisplayName = Some("Steve Randy Waldman"),
      newTagHost = None,
      newSelfHost = None,
      toFollowersAsPublic = true,
      sensitiveAsPublic = true,
      timestampTimezone = ZoneId.of("America/New_York")
    )

  def main(args : Array[String]) : Unit =
    val runner = new Runner(config)
    runner.main(args)

