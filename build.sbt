name := "amzport-utils"

version := "0.3.3"

lazy val `amzportutils` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  guice,
  caffeine,

  // sttp
  "com.softwaremill.sttp" %% "core" % "1.7.2",
  "com.softwaremill.sttp" %% "okhttp-backend" % "1.7.2",

  // monix
  "io.monix" %% "monix" % "3.1.0"

)
