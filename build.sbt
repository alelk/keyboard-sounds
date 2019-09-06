name := "keyboard-sound"

organization := "io.github.alelk"

version := "0.1"

scalaVersion := "2.13.0"

Compile / scalacOptions := Seq("-encoding", "UTF-8", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies += "com.1stleg" % "jnativehook" % "2.1.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.4"