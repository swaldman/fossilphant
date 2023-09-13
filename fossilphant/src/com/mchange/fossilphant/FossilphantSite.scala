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

  // no subdirectories! we generate theme resources into a single directory
  object GenUntemplates extends ZTEndpointBinding.Source:
    val BaseNameForHtmlRegex = """^(?:.*?)([^\.]*)_html_gen(?:|post|page)$""".r
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

    val typedGenPageUntemplates = typedThemeUntemplatesForSuffix[LocatedPageWithContext]( "_genpage" )

    def endpointBindingForGenPageUntemplate( untemplateFqn : String, untemplateFcn : untemplate.Untemplate[LocatedPageWithContext,Nothing] ) : Seq[ZTEndpointBinding] =
      untemplateFqn match
        case BaseNameForHtmlRegex(baseName) =>
          (0 until context.pages.length).map { index =>
            val locationBase = s"${baseName}_${index+1}" // index pages from one, not zero
            val location = Rooted(s"/${locationBase}.html")
            val task = ZIO.attempt {
              untemplateFcn(LocatedPageWithContext(location, index, context)).text
            }
            FossilphantSite.this.publicReadOnlyHtml(location, task, None, immutable.Set(locationBase, s"${locationBase}.html"), true, false)
          }
        case other =>
          throw new BadThemeUntemplate(s"'${other}' appears to be a theme untemplate that would generate an unknown or unexpected file type.")

    val endpointBindings =
      typedGenUntemplates.map( endpointBindingForGenUntemplate ).toSeq ++
      typedGenPostUntemplates.flatMap( endpointBindingForGenPostUntemplate ) ++
      typedGenPageUntemplates.flatMap( endpointBindingForGenPageUntemplate )

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
