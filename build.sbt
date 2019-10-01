name := "amzport-utils"
 
version := "0.0.1"
      
lazy val `amzportutils` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "Aliyun Releases" at "http://maven.aliyun.com/nexus/"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
      
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  specs2 % Test,
  guice,
  caffeine,

  // sttp
  "com.softwaremill.sttp" %% "core" % "1.6.5"
)
