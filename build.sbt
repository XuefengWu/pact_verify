lazy val root = (project in file(".")).
  settings(
    name := "pacts",
    version := "0.2",
    scalaVersion := "2.11.4"
  )

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("Typesafe Ivy Snapshots Repository", url("https://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns)


libraryDependencies ++= Seq(
  ("com.typesafe.play" %% "play-ws" % "2.4.0-M3").exclude("commons-logging", "commons-logging"),
  "com.typesafe.play" %% "play-json" % "2.4.0-M3",
  ("org.scala-lang.modules" %% "scala-xml" % "1.0.3").exclude("commons-logging", "commons-logging")
)
