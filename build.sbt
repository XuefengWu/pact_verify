lazy val root = (project in file(".")).
  settings(
    name := "pact_verify",
    version := "0.7.0",
    scalaVersion := "2.12.2"
  )

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  ("com.typesafe.play" %% "play-ws" % "2.6.1").exclude("commons-logging", "commons-logging").exclude("com.typesafe.play","build-link"),
  ("com.typesafe.play" %% "play-ahc-ws" % "2.6.1"),  
  ("com.typesafe.play" %% "play-json" % "2.6.1").exclude("com.typesafe.play","build-link"),  
  ("org.scala-lang.modules" %% "scala-xml" % "1.0.5").exclude("commons-logging", "commons-logging"),
  ("org.scalactic" %% "scalactic" % "3.0.1"),
  ("org.scalatest" %% "scalatest" % "3.0.1" % "test")
)

scalacOptions in Test ++= Seq("-Yrangepos")

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}