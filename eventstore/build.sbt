name := "duna-eventstore"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-async" % "0.9.7",
  "org.reactivestreams" % "reactive-streams" % "1.0.1",
  "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.4",

  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

scalacOptions ++= Seq(
  "-deprecation",
  "-Xlog-free-terms"
)