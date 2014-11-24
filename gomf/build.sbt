import play.Project._
import net.litola.SassPlugin

name := "gomf"

version := "1.0"

playScalaSettings

play.Project.playScalaSettings ++ SassPlugin.sassSettings

resolvers += "Scalate Snapshots" at "https://repository.jboss.org/nexus/content/repositories/fs-snapshots/"

libraryDependencies ++= Seq(
  cache,
  "eu.inn" %% "play2memcached" % "0.1",
  "org.fusesource.scalate" %% "scalate-core" % "1.7.0-SNAPSHOT"
)