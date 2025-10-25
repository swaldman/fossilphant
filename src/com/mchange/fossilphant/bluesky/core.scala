package com.mchange.fossilphant.bluesky

import zio.*

import scala.util.control.NonFatal

import com.mchange.bskyarchive.*
import com.mchange.fossilphant.MissingAvatar

private val DownloadRetrySchedule = Schedule.fixed( 30.seconds ) && Schedule.upTo( 2.minutes ) // XXX: hard-coded for now

case class DownloadStats( attempted : Int, succeeded : Int, failed : Int )

def downloadBskyArchiveImages( bskyRepoCar : os.Path, destinationDir : os.Path, verbose : Boolean, parallelism : Int = 5 ) : Task[DownloadStats] =
  val loadBskyArchive : Task[BskyArchive] = ZIO.attempt( BskyArchive( bskyRepoCar ) )
  val ensureDestinationDir : Task[Unit] = ZIO.attempt( os.makeDir.all(destinationDir) )

  def downloadImage( url : String, newImage : os.Path, ref : Ref[DownloadStats] ) : Task[Unit] =
    val rawDownload =
      ZIO.attemptBlocking:
        if verbose then
          java.lang.System.err.println( s"Downloading $url to $newImage." )
        os.write.over( newImage, requests.get.stream(url) )
        if !verbose then
          java.lang.System.err.print( '.' )
    val withStats = rawDownload.flatMap: _ =>
      ref.update( stats => stats.copy( attempted = stats.attempted + 1, succeeded = stats.succeeded + 1 ) )
    val withRetries =
      withStats.retry( DownloadRetrySchedule )
    withRetries.catchSome:
      case NonFatal(t) =>
        if verbose then
          java.lang.System.err.println()
          java.lang.System.err.println(s"Failed to download $url")
          t.printStackTrace()
        ref.update( stats => stats.copy( attempted = stats.attempted + 1, failed = stats.failed + 1 ) )

  def createDownloadTuples( bskyArchive : BskyArchive ) : Task[Set[Tuple2[String,os.Path]]] =
    def bskyImageUrlToDestPath( bskyImageUrl : String ) =
      val lastSlash = bskyImageUrl.lastIndexOf("/")
      val fname = bskyImageUrl.substring( lastSlash + 1 ).map( c => if c == '@' then '.' else c )
      os.Path( fname, destinationDir )
    ZIO.attemptBlocking:
      // special case avatar
      val avatar = 
        if bskyArchive.profile.blobRefs.size == 1 then
          val blobRef = bskyArchive.profile.blobRefs.head
          val href = blobRef.toBskyUrl( bskyArchive.did )
          val path = bskyImageUrlToDestPath( href )
          ( href, path )
        else
          throw new MissingAvatar(s"We expect one blob, the avatar image, in the profile record. Found ${bskyArchive.profile.blobRefs.size}: ${bskyArchive.profile}")
      bskyArchive.allImageUrls().map( url => (url, bskyImageUrlToDestPath( url ) ) ) + avatar

  for
    ref          <- Ref.make(DownloadStats(0,0,0))
    bskyArchive  <- loadBskyArchive
    _            <- ensureDestinationDir
    downloadTups <- createDownloadTuples( bskyArchive )
    _            <- ZIO.collectAllParDiscard( downloadTups.map( (url, path) => downloadImage( url, path, ref) ) ).withParallelism( parallelism )
    stats        <- ref.get
    _            <- ZIO.succeed( java.lang.System.err.println() ) // get past all the dots we've been printing
  yield stats
