package sbt

import sbt._
import Keys._
import scala.collection.mutable.ArrayBuffer
import java.io.{FileInputStream, FileOutputStream}


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

  lazy val jscoverReportsDir = SettingKey[File]("jsoverReportsDir", "Test reporting folder")

  lazy val jscoverReportDir = SettingKey[File]("jscoverReportDir", "Merge test folder")


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
    jscoverReportsDir <<= (thisProject) { project => project.base / "target/test-reports/jscoverReports"},
    jscoverReportDir <<= (thisProject) { project => project.base / "target/test-reports/jscoverReport"},
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

  /**
   * If there are several reports, merge them.
   * If there is only one report, rename it.
   * @return
   */
  def jscoverMergeTask = (streams, managedClasspath in jscoverConfig, classpathTypes, test in Test, thisProject, jscoverDestinationPath in jscoverConfig) map {
    (out, deps, ct, t, project, jscoverGeneratedFiles) =>
      val reportDirs:Array[File] = (project.base / "target/test-reports/jscover").listFiles()

      reportDirs.length match {
        case 0 => (project.base / "target/test-reports/jscoverReport").absolutePath
        case 1 => jscoverPrepareSingleReport(project.base / "target/test-reports/jscover", project.base / "target/test-reports/jscoverReport").getParentFile.absolutePath
        case _ => val destFolder:File = project.base / "target/test-reports/jscoverReport"
          if (!destFolder.exists()) {
            destFolder.mkdir()
          }
          (project.base / "target/test-reports/jscover").listFiles() foreach {f => copyFolder(jscoverGeneratedFiles / "original-src", f / "original-src") }
          deps.seq.foreach{ f =>
            f.map { file =>
              val exitCode = jscoverMergeReports(file.absolutePath,
                (project.base / "target/test-reports/jscover").listFiles(),
                destFolder.absolutePath,
                out.log)
            }
          }
          destFolder.absolutePath
      }
  }

  private def jscoverMergeReports(cp:String, reportDirs:Seq[File], destinationPath:String, log:Logger) = try {
    val proc = Process(
      "java",
      Seq[String]("-cp", cp, "jscover.report.Main", "--merge") ++ (reportDirs map { f:File => f.absolutePath }) ++ Seq(destinationPath)
    )
    log.info(proc.toString)
    proc ! log
  }

  private def copyFolder(from:File, to:File):Unit = {
    if (from.isDirectory) {
      if (!to.exists()) {
        to.mkdir()
      }
      if (to.isFile) {
        throw new IllegalStateException("Destination folder is a File")
      }
      from.listFiles().foreach{ f => copyFolder(f, new File(to, f.getName)) }
    } else {
      val fos = new FileOutputStream(to)
      fos getChannel() transferFrom(new FileInputStream(from) getChannel(), 0, Long.MaxValue)
      fos close()
    }
  }

  private def jscoverPrepareSingleReport(reportDir:File, destDir:File):File = {
    if (reportDir.listFiles().size != 1) {
      throw new IllegalStateException("There should be only one report folder")
    }
    if (reportDir.listFiles()(0).listFiles().size != 1) {
      throw new IllegalStateException("There should be only one report file")
    }
    val originalReport = reportDir.listFiles()(0).listFiles()(0)
    if (!destDir.exists()) {
      destDir.mkdir()
    }
    val finalReport = new File(destDir, "jscoverage.json")
    if (originalReport.renameTo(finalReport)) {
      finalReport
    } else {
      throw new IllegalStateException("Could not rename original report to final one")
    }
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
