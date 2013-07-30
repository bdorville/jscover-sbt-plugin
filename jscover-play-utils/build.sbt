name := "jscover-play-utils"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.0"

scalaBinaryVersion := CrossVersion.binaryScalaVersion("2.10.0")

crossScalaVersions := Seq("2.10.0")

seleniumVersion := "2.25.0"

playFrameworkVersion := "2.1.2"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies <+= (seleniumVersion) ("org.seleniumhq.selenium" % "selenium-remote-driver" % _ )

libraryDependencies <++= (playFrameworkVersion) { v =>
  Seq(
    "play" %% "play" % v % "provided",
    "play" %% "play-test" % v % "provided"
  )
}
