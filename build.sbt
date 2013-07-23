
name := "jscover-play"

version := "1.0-SNAPSHOT"

// Global Settings
organization in ThisBuild := "net.jazonnet.sbt.plugins"

scalacOptions in ThisBuild := Seq("-deprecation", "-unchecked")

publishTo in ThisBuild <<= (version) { version: String =>
  val scalasbt = "https://sample.org/artifactory/"
  val (name, url) = if (version.trim.endsWith("SNAPSHOT"))
                      ("sbt-plugins-snapshots", scalasbt + "plugins-snapshot-local")
                    else
                      ("sbt-plugin-releases", scalasbt + "plugins-release-local")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle in ThisBuild := false

credentials in ThisBuild += Credentials(Path.userHome / ".ivy2" / ".credentials")

