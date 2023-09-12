import $meta._

import mill._
import mill.scalalib._

import $ivy.`com.mchange::untemplate-mill:0.1.1-SNAPSHOT`
import untemplate.mill._

val UnstaticVersion = "0.1.0-SNAPSHOT"

object Dependency {
  val Unstatic       = ivy"com.mchange::unstatic:${UnstaticVersion}"
  val UnstaticZTapir = ivy"com.mchange::unstatic-ztapir:${UnstaticVersion}"
  val JArchiveLib    = ivy"org.rauschig:jarchivelib:1.2.0"
  val OsLib          = ivy"com.lihaoyi::os-lib:0.9.1"
  val Upickle          = ivy"com.lihaoyi::upickle:3.1.0" // last scala 3.2.x release
}

object fossilphant extends UntemplateModule {
  override def scalaVersion = "3.2.2"

  // we'll build an index!
  override def untemplateIndexNameFullyQualified : Option[String] = Some("com.mchange.fossilphant.IndexedUntemplates")

  override def untemplateSelectCustomizer: untemplate.Customizer.Selector = { key =>
    var out = untemplate.Customizer.empty

    // to customize, examine key and modify the customer
    // with out = out.copy=...
    //
    // e.g. out = out.copy(extraImports=Seq("com.mchange.fossilphant.*"))
    out = out.copy(extraImports=Seq("com.mchange.fossilphant.*"))

    out
  }

  override def ivyDeps = T {
    super.ivyDeps() ++
      Agg (
        Dependency.Unstatic,
        Dependency.UnstaticZTapir,
        Dependency.JArchiveLib,
        Dependency.OsLib,
        Dependency.Upickle,
      ) // Agg
  }
}


