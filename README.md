jscover-sbt-plugin
==================

This project is composed of
* a Play Framework SBT plugin that allows to generate instrumented JavaScript code, using JSCover
* a Play Framework utility library that provides helpers for generating JavaScript coverage reports when testing through Selenium

Play Framework SBT plugin
-------------------------

To use the SBT plugin, add the plugin to the `$playProject/project/plugins.sbt` file:

    addSbtPlugin("net.jazonnet.sbt.plugins" % "jscover-play-plugin" % "1.0-SNAPSHOT")

The defaults settings should be loaded in the `$playProject/project/Build.scala` file:

    object ApplicationBuild extends Build {
      ...
      lazy val s = Defaults.defaultSettings ++ JSCoverPlugin.jscoverSettings

      val main = play.Project(appName, appVersion, appDependencies, settings = s).settings(
        ...
      )
    }

Play Framework utility lib
--------------------------

The utility lib can be imported as an application dependency in `$playProject/project/Build.scala`:

    object ApplicationBuild extends Build {
      val appDependencies = Seq(
        ...
        "net.jazonnet.sbt.plugins" %% "jscover-play-utils" % "1.0-SNAPSHOT"
      )
    }