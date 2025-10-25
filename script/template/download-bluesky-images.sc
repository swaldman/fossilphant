
//> using scala "%SCALA_VERSION%"
//> using dep "com.monovore::decline:%DECLINE_VERSION%"
//> using dep "com.mchange::fossilphant:%FOSSILPHANT_VERSION%"
//> using jvm %JVM%

import java.time.*
import scala.collection.*
import com.monovore.decline.*
import com.monovore.decline.time.*
import com.mchange.fossilphant.*

import unstatic.UrlPath.Rooted
import unstatic.ztapir.ZTStaticGen

import cats.implicits.* // for mapN
import cats.data.{NonEmptyList,Validated,ValidatedNel}

import zio.*
import java.lang.System // so it's not shadowed by zio.System

import java.io.File.separatorChar

case class Config( repo : os.Path, outDir : os.Path, verbose : Boolean, parallelism : Int )

val repo = Opts.option[String]("repo", short="r", metavar="bluesky-repo-car", help="BlueSky repo.car for which images should be downloaded")
val outDir = Opts.option[String]("output", short="o", metavar="outdir", help="Directory into which to download images")
val verbose = Opts.flag("verbose",short="v",help="Log file downloads to stderr").orFalse
val parallelism = Opts.option[Int]("parallelism", short="p", help="Number of images to download in parallel").withDefault(5)

val allOpts : Opts[Config] =
  ( repo, outDir, verbose, parallelism ).mapN: (r, od, v, p ) =>
    Config( os.Path( r, os.pwd ), os.Path( od, os.pwd ), v, p )

val scriptName =
  if scriptPath.indexOf(separatorChar) >= 0 then
    val last = scriptPath.lastIndexOf(separatorChar)
    scriptPath.substring(last+1)
  else
    scriptPath

val command = Command(name=s"${scriptName}", header="Downloads all images referenced in a bluesky repository (repo.car) file.")( allOpts )

command.parse(args.toIndexedSeq, sys.env) match
  case Left(help) =>
    println(help)
    System.exit(1)
  case Right( config ) =>
    val task =
      for
        stats <- com.mchange.fossilphant.bluesky.downloadBskyArchiveImages( config.repo, config.outDir, config.verbose, config.parallelism )
      yield
        java.lang.System.err.println( s"${stats.succeeded} images downloaded (${stats.succeeded} failures, ${stats.failed} failures)" )
    Unsafe.unsafely:
      Runtime.default.unsafe.run(task).getOrThrow()

