package sbt

import sbt._
import Keys._
import scala.collection.mutable.ArrayBuffer


object JSCoverPlugin extends Plugin {

  ///////////////////////////////////////
  // JSCover resource generation
  final val jscoverConfig = config("jscover")

  lazy val jscoverSourcePath = SettingKey[File]("jscoverSourcePath", "The path of the resources to process through JSCover")

  lazy val jscoverDestinationPath = SettingKey[File]("jscoverDestinationPath", "The path where the resources processed through JSCover will be outputed")

  lazy val jscoverGenerate = TaskKey[Seq[File]]("jscoverGenerate")

  lazy val jscoverMergeReports = TaskKey[String]("jscoverMergeReports")

  lazy val jscoverReportFormat = SettingKey[String]("jscoverReportFormat", "The format for conversion (LCOV|COBERTURAXML|XMLSUMMARY)")

  lazy val jscoverFormatReport = TaskKey[String]("jscoverFormatReport", "Convert the report to another format")

  lazy val jscoverExcludes = SettingKey[Seq[String]]("jscoverExclusions", "List of folders to exclude for JSCover instrumentation")

  lazy val jscoverVersion = SettingKey[String]("jscoverVersion", "The version of JSCover used")


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
    jscoverMergeReports <<= jscoverMergeTask,
    jscoverReportFormat := "LCOV",
    jscoverFormatReport <<= jscoverFormat
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
      val reportDirs:Array[File] = (project.base / "target/test-reports/jscover").listFiles()

      reportDirs.length match {
        case 0 => (project.base / "target/test-reports/jscoverReport").absolutePath
        case 1 => (project.base / "target/test-reports/jscover").absolutePath
        case _ => deps.seq.foreach{ f =>
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
  }

  private def jscoverMergeReports(cp:String, reportDir:String, destinationPath:String, log:Logger) = try {
    val proc = Process(
      "java",
      Seq("-cp", cp, "jscover.report.Main", "--merge", reportDir, destinationPath)
    )
    log.info(proc.toString)
    proc ! log
  }

  // Format report
  def jscoverFormat = (streams, jscoverMergeReports, jscoverReportFormat, jscoverSourcePath, managedClasspath in jscoverConfig, classpathTypes) map {
    (out, reportDir, reportFormat, sourcePath, deps, ct) =>
      deps.seq.foreach{ f =>
        f.map { file =>
          val exitCode = jscoverFormatReport(reportFormat, file.absolutePath, reportDir, sourcePath.absolutePath, out.log)
        }
      }
      reportDir
  }

  private def jscoverFormatReport(format:String, cp:String, reportDir:String, sourceDir:String, log:Logger) = try {
    val proc = Process(
      "java",
      Seq("-cp", cp, "jscover.report.Main", "--format=" + format, reportDir, sourceDir)
    )
    log.info(proc.toString)
    proc ! log
  }
}
