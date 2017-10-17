name := "duna-perftest"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

enablePlugins(JmhPlugin)