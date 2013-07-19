sbtPlugin := true

organization := "net.jazonnet.sbt.plugins"

name := "jscover-sbt-plugin"

version := "1.0-SNAPSHOT"

scalacOptions := Seq("-deprecation", "-unchecked")

publishTo <<= (version) { version: String =>
  val scalasbt = "https://sample.org/artifactory/"
  val (name, url) = if (version.trim.endsWith("SNAPSHOT"))
                      ("sbt-plugins-snapshots", scalasbt + "plugins-snapshot-local")
                    else
                      ("sbt-plugin-releases", scalasbt + "plugins-release-local")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

