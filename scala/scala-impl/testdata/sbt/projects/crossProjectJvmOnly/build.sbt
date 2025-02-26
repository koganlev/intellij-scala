ThisBuild / scalaVersion := "2.13.16"

lazy val root = project.in(file("."))
  .aggregate(/*p1.js, */p1.jvm)
  .settings(
    name := "root"
  )

lazy val p1 = crossProject(/*JSPlatform, */JVMPlatform).in(file("p1"))
