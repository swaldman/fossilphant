#!/usr/bin/env -S scala-cli shebang

// expects SONATYPE_USERNAME and SONATYPE_PASSWORD to be set prior to running!

val console = System.console()
if (console == null) {
  System.err.println("Could not initialize system console. Exiting.")
  System.exit(1)
}

val sonatypeUsername = System.getenv("SONATYPE_USERNAME")
val sonatypePassword = System.getenv("SONATYPE_PASSWORD")
if (sonatypeUsername == null) {
  System.err.println(s"Environment variable 'SONATYPE_USERNAME' not set. Exiting.")
  System.exit(1)
}
if (sonatypePassword == null) {
  System.err.println(s"Environment variable 'SONATYPE_PASSWORD' not set. Exiting.")
  System.exit(1)
}

val gpgPassphrase = console.readPassword("Please enter GPG passphrase for signing: ")

val argsPreparsed = Seq(
  "./mill", "mill.scalalib.PublishModule/publishAll", "__.publishArtifacts",
  "--sonatypeCreds", s"${sonatypeUsername}:${sonatypePassword}",
  "--gpgArgs", s"--passphrase=${new String(gpgPassphrase)},--batch,--yes,-a,-b",
  "--release", "false"
)

import scala.sys.process._
argsPreparsed.!
