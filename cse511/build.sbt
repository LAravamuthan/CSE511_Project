import sbt.Keys.{libraryDependencies, scalaVersion, version}


lazy val root = (project in file(".")).
  settings(
    name := "cse511",

    version := "0.1",

    scalaVersion := "2.11.11",

    publishMavenStyle := true,

    mainClass in (Compile, run) := Some("phase1.SparkSQLExample")
  )

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.2.0",
  "org.apache.spark" %% "spark-sql" % "2.2.0",
  "org.scalatest" %% "scalatest" % "2.2.4",
  "org.specs2" %% "specs2-core" % "2.4.16",
  "org.specs2" %% "specs2-junit" % "2.4.16"
)