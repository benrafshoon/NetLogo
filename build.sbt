scalaVersion := "2.9.2"

name := "NetLogo"

onLoadMessage := ""

resourceDirectory in Compile <<= baseDirectory(_ / "resources")

scalacOptions ++=
  "-deprecation -unchecked -Xfatal-warnings -Xcheckinit -encoding us-ascii"
  .split(" ").toSeq

javacOptions ++=
  "-g -deprecation -encoding us-ascii -Werror -Xlint:all -Xlint:-serial -Xlint:-fallthrough -Xlint:-path -source 1.6 -target 1.6"
  .split(" ").toSeq

// only log problems plz
ivyLoggingLevel := UpdateLogging.Quiet

// we're not cross-building for different Scala versions
crossPaths := false

scalaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

javaSource in Compile <<= baseDirectory(_ / "src" / "main")

javaSource in Test <<= baseDirectory(_ / "src" / "test")

unmanagedSourceDirectories in Test <+= baseDirectory(_ / "src" / "tools")

unmanagedResourceDirectories in Compile <+= baseDirectory { _ / "resources" }

mainClass in (Compile, run) := Some("org.nlogo.headless.Main")

mainClass in (Compile, packageBin) := Some("org.nlogo.headless.Main")

sourceGenerators in Compile <+= Autogen.sourceGeneratorTask

resourceGenerators in Compile <+= I18n.resourceGeneratorTask

Depend.dependTask

libraryDependencies ++= Seq(
  "asm" % "asm-all" % "3.3.1",
  "org.picocontainer" % "picocontainer" % "2.13.6",
  "org.jmock" % "jmock" % "2.5.1" % "test",
  "org.jmock" % "jmock-legacy" % "2.5.1" % "test",
  "org.jmock" % "jmock-junit4" % "2.5.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
  "org.scalatest" %% "scalatest" % "1.8" % "test"
)
