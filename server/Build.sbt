organization  := "bondor.sergiu"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.3"
  val specs2 = "2.4.17"
  val json4s = "3.2.11"

  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV    % "test",

    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV     % "test",
    "com.typesafe.akka"   %%  "akka-slf4j"    % akkaV,

    "org.specs2"          %%  "specs2-core"   % specs2    % "test",
    "org.specs2"          %%  "specs2-mock"   % specs2    % "test",
    "org.specs2"          %%  "specs2-matcher-extra" % specs2 % "test",
    "org.mockito"         %   "mockito-core"  % "1.10.19" % "test",

    "org.json4s"          %%  "json4s-native" % json4s,
    "org.json4s"          %%  "json4s-ext"    % json4s,
    "org.json4s"          %%  "json4s-jackson" % json4s,
    
    "com.typesafe.slick"  %%  "slick"         % "3.0.0",
    "org.postgresql"      %   "postgresql"    % "9.4-1201-jdbc41",
    
    "ch.qos.logback"      %   "logback-core"  % "1.1.3",
    "ch.qos.logback"      %   "logback-classic" % "1.1.3",
    "org.slf4j"           %   "slf4j-api"     % "1.7.12",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",

    "joda-time"           %   "joda-time"     % "2.7",
    "commons-io"          %   "commons-io"    % "2.4"
  )
}

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "Spray repo" at "http://repo.spray.io/",
  "Akka repo" at "http://repo.akka.io/releases/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
)

Revolver.settings