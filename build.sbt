name := """play-web-app"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.3"

libraryDependencies ++= Seq( guice,
                             "com.h2database" % "h2" % "1.4.196",
                             "com.github.mauricio" %% "postgresql-async" % "0.2.21" )

