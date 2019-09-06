name := "keyboard-sound"

organization := "io.github.alelk"
maintainer := "alelkdev@gmail.com"

version := "0.1"

scalaVersion := "2.13.0"

Compile / scalacOptions := Seq("-encoding", "UTF-8", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies += "com.1stleg" % "jnativehook" % "2.1.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.4"

// SBT-Native-Packager settings
import NativePackagerHelper._
enablePlugins(JavaAppPackaging)
mappings in Universal ++= directory("sounds") map {case (f, fName) =>
  f -> s"bin/$fName"
}