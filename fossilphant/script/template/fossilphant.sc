
//> using scala "%SCALA_VERSION%"
//> using dep "com.monovore::decline:%DECLINE_VERSION%"
//> using dep "com.mchange::fossilphant:%FOSSILPHANT_VERSION%"
//> using jvm %JVM%

import java.time.*
import scala.collection.*
import com.monovore.decline.*
import com.monovore.decline.time.*
import com.mchange.fossilphant.*
import unstatic.ztapir.ZTStaticGen

import cats.implicits.* // for mapN
import cats.data.{NonEmptyList,Validated,ValidatedNel}

val validThemes = immutable.SortedSet("shatter","tower")
val themeMetaVar = validThemes.mkString("|")

val includeFollowersOnly = Opts.flag("include-followers-only", help="Include posts sent to all followers but not the full public").orFalse
val includeSensitive     = Opts.flag("include-sensitive", help="Include posts marked sensitive").orFalse
val outDir = Opts.option[String]("output", short="o", metavar="outdir", help="Directory into which to generate site").withDefault("public")
val pageLength = Opts.option[Int]("page-length", help="Number of posts per page (for themes that support paging)").withDefault(20)
val selfUrl = Opts.option[String]("self-url", metavar="url", help="URL to which you'd like your display name and handle to link (if not to your profile on the archived server)ß").withDefault("null").map( Option.apply ) // the null is very transient, becomes None
val tagHost = Opts.option[String]("tag-host", metavar="hostname", help="Mastodon instance to which hashtag links should be directed (if not to the archived instance)").withDefault(null).map( Option.apply ) // the null is very transient, becomes None
val tagline = Opts.option[String]("tagline", help="Main tagline for the generated site").withDefault(null).map( Option.apply ) // the null is very transient, becomes None
val theme = Opts.option[String]("theme", short="t", metavar=themeMetaVar, help="Name of theme for generated site").withDefault("shatter").validate(s"""Theme must be one of ${validThemes.mkString(", ")}""")( t => validThemes(t) )

// modified from decline's docs
val themeConfig : Opts[immutable.Map[String,String]] =
  def validate( strings : List[String] ) : ValidatedNel[String,List[Tuple2[String,String]]] =
    strings.map { s =>
      s.split(":", 2) match {
          case Array(key, value) => Validated.valid(Tuple2(key, value))
          case _ => Validated.invalidNel(s"Invalid key:value pair: ${s}")
      }
    }.sequence

  Opts.options[String]("theme-config", "Specify a configuration parameter for your theme.", metavar = "key:value")
    .map( _.toList)
    .withDefault(Nil)
    .mapValidated( validate )
    .map( immutable.Map.from )
end themeConfig

val timezone = Opts.option[ZoneId]("timezone", help="Timezone to use when generating post timestamps").withDefault(ZoneId.systemDefault())
val title = Opts.option[String]("title", help="Main title for the generated site").withDefault(null).map( Option.apply ) // the null is very transient, becomes None
val archive = Opts.argument[String](metavar = "archive-tar-gz-or-dir") //, help = "a tar.gz file exported by Mastodon or its extracted directory")

val allOpts : Opts[Tuple2[FossilphantConfig,os.Path]] =
  (includeFollowersOnly, includeSensitive, outDir, pageLength, selfUrl, tagHost, tagline, theme, themeConfig, timezone, title, archive).mapN { (ifo, is, od, pl, su, th, ta, tn, tc, tz, ti, a) =>
    val config = FossilphantConfig(
      toFollowersAsPublic=ifo,
      sensitiveAsPublic=is,
      pageLength = pl,
      newSelfUrl = su,
      contentTransformer = th.fold(identity : String => String )( ContentTransformer.rehostTags(_) ),
      mainTagline = ta,
      themeName = tn,
      themeConfig = tc,
      timestampTimezone = tz,
      mainTitle = ti,
      archivePath = Some( a )
    )
    val outPath = os.Path( od, os.pwd )
    (config, outPath)
  }

val command = Command(name=s"${scriptPath}", header="Generates a static site from a Mastodon archive")( allOpts )

command.parse(args.toIndexedSeq, sys.env) match
  case Left(help) =>
    println(help)
    System.exit(1)
  case Right( (config, outPath) ) =>
    import zio.*
    val site = FossilphantSite( config )
    val task = ZTStaticGen.generateZTSite( site, outPath.toNIO )
    Unsafe.unsafely:
      Runtime.default.unsafe.run(task.debug).getOrThrow()
