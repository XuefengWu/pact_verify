lazy val root = (project in file(".")).
  settings(
    name := "pact_verify",
    version := "0.8.4",
    scalaVersion := "2.12.2"
  )

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  ("org.apache.httpcomponents" % "httpclient" % "4.5.3"),
  ("com.typesafe.play" %% "play-json" % "2.6.1").exclude("com.typesafe.play","build-link"),  
  ("org.scala-lang.modules" %% "scala-xml" % "1.0.5").exclude("commons-logging", "commons-logging"),
  ("org.scalactic" %% "scalactic" % "3.0.1" % "test"),
  ("org.scalatest" %% "scalatest" % "3.0.1" % "test")
)

scalacOptions in Test ++= Seq("-Yrangepos")

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}