name := "amzport-utils"

version := "0.0.1"

lazy val `amzportutils` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "Aliyun Releases" at "http://maven.aliyun.com/nexus/"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
      
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  jdbc,
  ws,
  specs2 % Test,
  guice,
  caffeine,

  // play & slick bind
  "com.typesafe.play" %% "play-slick" % "4.0.0",
  "com.typesafe.slick" %% "slick-codegen" % "3.3.2",

  // postgresql
  "org.postgresql" % "postgresql" % "42.2.5",

  // slick-pg extension
  "com.github.tminglei" %% "slick-pg" % "0.17.2",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.17.2",

  // sttp
  "com.softwaremill.sttp" %% "core" % "1.6.5",

  // hash helper
  "com.roundeights" %% "hasher" % "1.2.0"

)

      