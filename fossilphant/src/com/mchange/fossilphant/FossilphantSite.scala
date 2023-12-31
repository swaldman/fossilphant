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

class FossilphantSite( val config : FossilphantConfig ) extends ZTSite.Composite:

  // directory beneath which hash-special media-dir content can be
  // checked to make sure media-dir links have referents
  //
  // we have no need for this... theme templates may use hash special anchor links
  // or links to site-rooted paths, but not media-dir links
  // our endpoints do not define media-dirs, so attempts to reference them will throw anyway.
  override val enforceUserContentFrom : Option[immutable.Seq[JPath]] = None

  // these are... not so good. But this site should produce only
  // relative paths, so it should not matter
  override val serverUrl : Abs    = Abs("https://www.unknown.com/")
  override val basePath  : Rooted = Rooted.root

  // make this a command line arg soon!
  val archiveLoc : String =
    config.archivePath.getOrElse(
      throw new BadArchivePath(s"Location of Mastodon archive is defined neither in ${Env.Archive} environment variable nor in config.")
    )

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

  lazy val context = FossilphantContext( archiveDir, config )

  // no subdirectories! we generate theme resources into a single directory
  object GenUntemplates extends ZTEndpointBinding.Source:
    val BaseNameForHtmlRegex = """^(?:.*?)([^\.]*)_html_gen(?:|post|pagewithall|pagewithothers|pagewithout)$""".r
    val BaseNameForCssRegex  = """^(?:.*?)([^\.]*)_css_gen""".r

    val themeIndex = IndexedUntemplates

    def typedThemeUntemplatesForSuffix[INPUT]( suffix : String ) : Map[String,untemplate.Untemplate[INPUT,Nothing]] =
      val suffixUntemplates = themeIndex.filter( (k,_) => k.startsWith("com.mchange.fossilphant.theme." + config.themeName) && k.endsWith(suffix) )

      // the compiler isn't reall able to check these properly because of type erasure, alas
      val typedSuffixUntemplates = suffixUntemplates.collect { case tup : (String, untemplate.Untemplate[INPUT,Nothing]) => tup  }

      if suffixUntemplates.size != typedSuffixUntemplates.size then
        val badlyTypedSuffixUntemplates = suffixUntemplates.keySet.removedAll(typedSuffixUntemplates.keySet)
        scribe.warn(s"""The following theme untemplates were badly typed and will be ignored: ${badlyTypedSuffixUntemplates.mkString(", ")}""")

      typedSuffixUntemplates
    end typedThemeUntemplatesForSuffix

    val typedGenUntemplates = typedThemeUntemplatesForSuffix[LocatedContext]( "_gen" )

    def endpointBindingForGenUntemplate( untemplateFqn : String, untemplateFcn : untemplate.Untemplate[LocatedContext,Nothing] ) : ZTEndpointBinding =
      untemplateFqn match
        case BaseNameForHtmlRegex(baseName) =>
          val location = Rooted(s"/${baseName}.html")
          val task = ZIO.attempt {
            untemplateFcn(LocatedContext(location,context)).text
          }
          FossilphantSite.this.publicReadOnlyHtml( location, task, None, immutable.Set(baseName,s"${baseName}.html"), true, false )
        case BaseNameForCssRegex(baseName) =>
          val location = Rooted(s"/${baseName}.css")
          val task = ZIO.attempt {
            untemplateFcn(LocatedContext(location,context)).text
          }
          ZTEndpointBinding.publicReadOnlyCss( location, FossilphantSite.this, task, None, immutable.Set(baseName,s"${baseName}.css") )
        case other =>
          throw new BadThemeUntemplate(s"'${other}' appears to be a theme untemplate that would generate an unknown or unexpected file type.")

    val typedGenPostUntemplates = typedThemeUntemplatesForSuffix[LocatedPostWithContext]( "_genpost" )

    def endpointBindingForGenPostUntemplate( untemplateFqn : String, untemplateFcn : untemplate.Untemplate[LocatedPostWithContext,Nothing] ) : Seq[ZTEndpointBinding] =
      untemplateFqn match
        case BaseNameForHtmlRegex(baseName) =>
          context.reverseChronologicalPublicPosts.map { post =>
            val locationBase = s"${baseName}_${post.localId}"
            val location = Rooted(s"/${locationBase}.html")
            val task = ZIO.attempt {
              untemplateFcn(LocatedPostWithContext(location, post, context)).text
            }
            FossilphantSite.this.publicReadOnlyHtml(location, task, None, immutable.Set(locationBase, s"${locationBase}.html"), true, false)
          }
        case other =>
          throw new BadThemeUntemplate(s"'${other}' appears to be a theme untemplate that would generate an unknown or unexpected file type.")

    val typedGenPageWithAllUntemplates = typedThemeUntemplatesForSuffix[LocatedPageWithContext]( "_genpagewithall" )
    val typedGenPageWithOthersUntemplates = typedThemeUntemplatesForSuffix[LocatedPageWithContext]( "_genpagewithothers" )
    val typedGenPageWithoutUntemplates = typedThemeUntemplatesForSuffix[LocatedPageWithContext]( "_genpagewithout" )

    def endpointBindingForGenPageUntemplate(pst : PostSeqType)( untemplateFqn : String, untemplateFcn : untemplate.Untemplate[LocatedPageWithContext,Nothing] ) : Seq[ZTEndpointBinding] =
      untemplateFqn match
        case BaseNameForHtmlRegex(baseName) =>
          val pages = pst match
            case PostSeqType.WithAll    => context.pagesIncludingReplies
            case PostSeqType.WithOthers => context.pagesIncludingRepliesToOthersOnly
            case PostSeqType.Without    => context.pagesNoReplies
          (0 until pages.size).map { index =>
            val locationBase = s"${baseName}_${index+1}" // index pages from one, not zero
            val location = Rooted(s"/${locationBase}.html")
            val task = ZIO.attempt {
              untemplateFcn(LocatedPageWithContext(location, index, pages, context)).text
            }
            FossilphantSite.this.publicReadOnlyHtml(location, task, None, immutable.Set(locationBase, s"${locationBase}.html"), true, false)
          }
        case other =>
          throw new BadThemeUntemplate(s"'${other}' appears to be a theme untemplate that would generate an unknown or unexpected file type.")

    lazy val endpointBindings =
      typedGenUntemplates.map( endpointBindingForGenUntemplate ).toSeq ++
      typedGenPostUntemplates.flatMap( endpointBindingForGenPostUntemplate ) ++
      typedGenPageWithAllUntemplates.flatMap( endpointBindingForGenPageUntemplate( PostSeqType.WithAll ) ) ++
      typedGenPageWithOthersUntemplates.flatMap( endpointBindingForGenPageUntemplate( PostSeqType.WithOthers ) ) ++
      typedGenPageWithoutUntemplates.flatMap( endpointBindingForGenPageUntemplate( PostSeqType.Without ) )

    require( !endpointBindings.isEmpty, s"No resources were found for configured theme '${config.themeName}'!")

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
    lazy val endpointBindings = mediaAttachmentsEndpointBinding :: avatarStaticBinding :: Nil
  end StaticArchiveResources

  object ClassLoaderResources extends ZTEndpointBinding.Source:
    val genResourcePaths = List(
      "font/Montserrat/Montserrat-VariableFont_wght.ttf",
      "font/Montserrat/Montserrat-Italic-VariableFont_wght.ttf"
    )
    def ttfBindingFromPath( path : String ) : ZTEndpointBinding =
      val siteRootedPath = Rooted.parseAndRoot( path )
      ZTEndpointBinding.fromClassLoaderResource( siteRootedPath, FossilphantSite.this, this.getClass.getClassLoader, path, "font/ttf", immutable.Set.empty )

    lazy val endpointBindings = genResourcePaths.map( ttfBindingFromPath )
  end ClassLoaderResources  

  // avoid conflicts, but early items in the lists take precedence over later items
  override lazy val endpointBindingSources : immutable.Seq[ZTEndpointBinding.Source] = immutable.Seq( StaticArchiveResources, GenUntemplates, ClassLoaderResources )
