lazy val root = (project in file(".")).
  settings(
    name := "pacts",
    version := "0.3",
    scalaVersion := "2.11.4"
  )

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("Typesafe Ivy Snapshots Repository", url("https://repo.typesafe.com/typesafe/ivy-snapshots"))(Resolver.ivyStylePatterns)


libraryDependencies ++= Seq(
  ("com.typesafe.play" %% "play-ws" % "2.3.1").exclude("commons-logging", "commons-logging").exclude("com.typesafe.play","build-link"),
  ("com.typesafe.play" %% "play-json" % "2.3.1").exclude("com.typesafe.play","build-link"),
  ("org.scala-lang.modules" %% "scala-xml" % "1.0.3").exclude("commons-logging", "commons-logging")
)
