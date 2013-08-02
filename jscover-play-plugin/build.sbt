sbtPlugin := true

name := "jscover-play-plugin"

version := "1.0-SNAPSHOT"

//scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.10")

scalacOptions := Seq("-deprecation", "-unchecked")

// Scripted plugin for tests
//addSbtPlugin("org.scala-sbt" % "scripted-plugin" % "0.12.4", "", "")

//ScriptedPlugin.scriptedSettings