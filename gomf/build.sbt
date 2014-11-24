import play.Project._
import net.litola.SassPlugin

name := "gomf"

version := "1.0"

playScalaSettings

play.Project.playScalaSettings ++ SassPlugin.sassSettings

libraryDependencies ++= Seq(
  cache,
  "eu.inn" %% "play2memcached" % "0.1"
)