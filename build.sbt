
name := "jscover-play"

version := "1.0-SNAPSHOT"

// Global Settings
organization in ThisBuild := "net.jazonnet.sbt.plugins"

scalacOptions in ThisBuild := Seq("-deprecation", "-unchecked")

scalaSbtRepository in ThisBuild := "https://sample.org/artifactory/"

publishTo in ThisBuild <<= (version, scalaSbtRepository) { (version: String, scalasbt: String) =>
  val (name, url) = if (version.trim.endsWith("SNAPSHOT"))
                      ("sbt-plugins-snapshots", scalasbt + "plugins-snapshot-local")
                    else
                      ("sbt-plugin-releases", scalasbt + "plugins-release-local")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle in ThisBuild := false

credentials in ThisBuild += Credentials(Path.userHome / ".ivy2" / ".credentials")

