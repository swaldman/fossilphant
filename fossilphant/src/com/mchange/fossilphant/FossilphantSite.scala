package com.mchange.fossilphant

import scala.collection.*

import unstatic.*
import unstatic.ztapir.*
import unstatic.ztapir.simple.*

import unstatic.*, UrlPath.*

import java.nio.file.Path as JPath

import untemplate.Untemplate.AnyUntemplate

import zio.*

object FossilphantSite extends ZTSite.SingleRootComposite( JPath.of("fossilphant/static") ):

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
  val unsortedPosts = grouped("Create")

  val reverseChronologicalPosts =
    immutable.SortedSet.from( unsortedPosts )( ReverseChronologicalPublished ).toList
  val context = FossilphantContext( outbox.obj, reverseChronologicalPosts )
  // customize this to what the layout you want requires!
  case class MainLayoutInput( renderLocation : SiteLocation, mbTitle : Option[String], mainContentHtml : String )

  val HtmlRegex = """^(?:.*?)([^\.]*)_html_gen$""".r
  val CssRegex  = """^(?:.*?)([^\.]*)_css_gen""".r

  // no subdirectories! we generate theme resources into a single directory
  object GenUntemplates extends ZTEndpointBinding.Source:
    val themeIndex = IndexedUntemplates
    val genUntemplates = themeIndex.filter( (k,_) => k.endsWith("_gen") )

    val typedGenUntemplates = genUntemplates.collect {
      case tup : (String, untemplate.Untemplate[FossilphantContext,Nothing]) => tup
    }

    if genUntemplates.size != typedGenUntemplates.size then
      val badlyTypedGenUntemplates = genUntemplates.keySet.removedAll(typedGenUntemplates.keySet)
      scribe.warn(s"""The following theme untemplates were badly types and will be ignored: ${badlyTypedGenUntemplates.mkString(", ")}""")

    def endpointBindingForGenUntemplate( tup : (String, untemplate.Untemplate[FossilphantContext,Nothing]) ) : ZTEndpointBinding =
      tup(0) match
        case HtmlRegex(baseName) =>
          val location = FossilphantSite.location(s"/${baseName}.html")
          val task = ZIO.attempt {
            tup(1)(context).text
          }
          ZTEndpointBinding.publicReadOnlyHtml( location, task, None, immutable.Set(baseName,s"${baseName}.html") )
        case CssRegex(baseName) =>
          val location = FossilphantSite.location(s"/${baseName}.css")
          val task = ZIO.attempt {
            tup(1)(context).text
          }
          ZTEndpointBinding.publicReadOnlyCss( location, task, None, immutable.Set(baseName,s"${baseName}.css") )
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
      ZTEndpointBinding.staticDirectoryServing( FossilphantSite.location(mediaAttachmentsDirName), mediaAttachmentsPath.toNIO, immutable.Set(mediaAttachmentsDirName) )
    val avatarStaticBinding =
      ZTEndpointBinding.staticFileServing( FossilphantSite.location(avatarFileName), avatarPath.toNIO, immutable.Set("avatar", avatarFileName) )
    val endpointBindings = mediaAttachmentsEndpointBinding :: avatarStaticBinding :: Nil
  end StaticArchiveResources

/*
  // get rid of this -- modify it into something useful and/or include something like a SimpleBlog defined as a singleton object
  object IndexPage extends ZTEndpointBinding.Source:
    val location = FossilphantSite.location("/index.html")
    val task = zio.ZIO.attempt {
      val text =
        posts.map(postMap => """<div class="post">""" + postMap("object").obj("content").str + """</div>""").mkString("\n")
      layout_main_html( MainLayoutInput( location, Some("Hello"), text ) ).text
    }
    val endpointBindings = ZTEndpointBinding.publicReadOnlyHtml( location, task, None, immutable.Set("index","index.html") ) :: Nil
  end IndexPage
*/
  // avoid conflicts, but early items in the lists take precedence over later items
  override val endpointBindingSources : immutable.Seq[ZTEndpointBinding.Source] = immutable.Seq( StaticArchiveResources, GenUntemplates )

object FossilphantSiteGenerator extends ZTMain(FossilphantSite, "fossilphant-site")

