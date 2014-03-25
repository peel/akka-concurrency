name := "avionics"

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++=
    "com.typesafe.akka" %% "akka-actor" % "2.3.0" ::
    "com.typesafe.akka" %% "akka-testkit" % "2.3.0" % "test" ::
    "com.typesafe.akka" %% "akka-remote" % "2.3.0" ::
    "org.scalatest" %% "scalatest" % "2.1.2" % "test" ::
    Nil

