import sbt._
import Keys._

object JSCoverPlayPluginBuild extends Build {

  /**
   * Define custom keys
   */
  lazy val scalaSbtRepository = SettingKey[String]("scala-sbt-repository", "The publishing repository URL")

  lazy val seleniumVersion = SettingKey[String]("selenium-version", "The version of Selenium used")

  lazy val playFrameworkVersion = SettingKey[String]("play-version", "The version of Play Framework")

  // Projects definitions
  lazy val jscoverPlayUtils = Project(id = "jscover-play-utils", base = file("jscover-play-utils"))

  lazy val jscoverPlayPlugin = Project(id = "jscover-play-plugin", base = file("jscover-play-plugin"))

  lazy val root = Project(id = "jscover-play", base  = file(".")) aggregate(jscoverPlayPlugin, jscoverPlayUtils)

}
