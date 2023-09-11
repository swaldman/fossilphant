import mill._, scalalib._

object millbuild extends MillBuildRootModule {
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Ytasty-reader")
  }
}

