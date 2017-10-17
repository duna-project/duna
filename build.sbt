name := "duna"
version := "0.1"

scalaVersion := "2.12.3"

val `duna-eventstore` = project in file("eventstore")

val `duna-perftest` = (project in file("perftest"))
  .dependsOn(`duna-eventstore`)
  .enablePlugins(JmhPlugin)

val duna = (project in file("."))
  .aggregate(`duna-eventstore`)