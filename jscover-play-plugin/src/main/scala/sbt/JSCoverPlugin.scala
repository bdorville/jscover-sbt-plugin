package sbt

import sbt._
import Keys._
import scala.collection.mutable.ArrayBuffer


object JSCoverPlugin extends Plugin {

  ///////////////////////////////////////
  // JSCover resource generation
  final val jscoverConfig = config("jscover")

  val jscoverSourcePath = SettingKey[File]("jscoverSourcePath", "The path of the resources to process through JSCover")

  val jscoverDestinationPath = SettingKey[File]("jscoverDestinationPath", "The path where the resources processed through JSCover will be outputed")

  val jscoverGenerate = TaskKey[Seq[File]]("jscoverGenerate")

  val jscoverMergeReports = TaskKey[String]("jscoverMergeReports")

  val jscoverExcludes = SettingKey[Seq[String]]("jscoverExclusions", "List of folders to exclude for JSCover instrumentation")

  val jscoverVersion = SettingKey[String]("jscoverVersion", "The version of JSCover used")


  // Define the settings values in a specific SBT configuration
  lazy val jscoverSettings: Seq[Setting[_]] = inConfig(jscoverConfig)(Seq[Setting[_]] (
    jscoverSourcePath <<= (resourceManaged in Compile) {_ / "public/javascripts"},
    jscoverDestinationPath <<= (resourceManaged in Compile) {_ / "public/jscover/javascripts"},
    jscoverVersion := "0.3.1",
    classpathTypes := Set("jar"),
    jscoverExcludes := Seq[String]("bootstrap"),
    managedClasspath <<= (classpathTypes, update) map { (ct, report) =>
      Classpaths.managedJars(jscoverConfig, ct, report)
    },
    jscoverGenerate <<= jscoverGenerateTask,
    resourceGenerators in Compile <+= jscoverGenerate,
    jscoverMergeReports <<= jscoverMergeTask
  )) ++ Seq[Setting[_]] (
    libraryDependencies <+= (jscoverVersion in jscoverConfig)("com.github.tntim96" % "JSCover" % _ % "jscover"),
    ivyConfigurations += jscoverConfig
  )

  def jscoverGenerateTask = (streams, managedClasspath in jscoverConfig, classpathTypes, update, jscoverSourcePath, jscoverDestinationPath, jscoverExcludes, cacheDirectory) map {
    (out, deps, ct, reports, sourcePath, destinationPath, exclusions, c) =>
      val res = ArrayBuffer[File]()

      val cacheFile = c / "jscover"
      val currentInfos = (sourcePath ** "*.js").get.map(f => f -> FileInfo.lastModified(f)).toMap
      val (previousRelation, previousInfos) = Sync.readInfo(cacheFile)(FileInfo.lastModified.format)

      if (previousInfos != currentInfos) {

        deps.seq.foreach { f =>
          f.map{ ff =>
            out.log.debug("Dependency: %s".format(ff.absolutePath))
            val exitCode = jscoverGenerateInstrumentedApp(ff.absolutePath, sourcePath.absolutePath, destinationPath.absolutePath, exclusions, out.log)
            // Add the generated JSCover files to the managed-resources setting
//            if (exitCode != 0) {
//              res ++= listFileChildren(destinationPath)
//            } else {
//              res ++ previousRelation._2s.toSeq
//            }
          }
        }

        Sync.writeInfo(cacheFile,
          // TODO manage the relations src -> target
          Relation.empty[File, File],
          currentInfos)(FileInfo.lastModified.format)

      } else {
        res ++ previousRelation._2s.toSeq
      }
      // for now add all the elements in the destination folder
      res ++= listFileChildren(destinationPath)
      res.toSeq
  }

  private def jscoverGenerateInstrumentedApp(cp:String, sourcePath:String, destinationPath:String, exclusions:Seq[String] = Seq[String](), log:Logger) =
    try {
      val proc = Process(
        "java",
        Seq("-jar", cp, "-fs") ++ (exclusions map(e => "--no-instrument=" + e)) ++ Seq(sourcePath, destinationPath)
      )
      log.info(proc.toString)
      proc ! log
    }

  private def listFileChildren(origin:File):Seq[File] = {
    val a = ArrayBuffer[File](origin)
    if (origin.isDirectory) {
      origin.listFiles().foreach { child =>
        a ++= listFileChildren(child)
      }
    }
    a.toSeq
  }

  // Merging
  def jscoverMergeTask = (streams, managedClasspath in jscoverConfig, classpathTypes, test in Test, thisProject) map {
    (out, deps, ct, t, project) =>
      deps.seq.foreach{ f =>
        f.map { file =>
          (project.base / "target/test-reports/jscoverReport").absolutePath
          val exitCode = jscoverMergeReports(file.absolutePath,
                                             (project.base / "target/test-reports/jscover").absolutePath,
                                             (project.base / "target/test-reports/jscoverReport").absolutePath,
                                             out.log)
        }
      }
      (project.base / "target/test-reports/jscoverReport").absolutePath
  }

  private def jscoverMergeReports(cp:String, reportDir:String, destinationPath:String, log:Logger) = try {
    val proc = Process(
      "java",
      Seq("-jar", cp, "jscover.report.Main", "--merge", reportDir, destinationPath)
    )
    log.info(proc.toString)
    proc ! log
  }
}
