addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.6.0-RC4")
libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
