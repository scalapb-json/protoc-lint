import sbtrelease.ReleaseStateTransformations._

val Scala210 = "2.10.7"
val Scala212 = "2.12.4"
val sbt013 = "0.13.16"

commands += BasicCommands.newAlias(
  "setSbt013",
  s"""; ^^ ${sbt013} ; set scalaVersion := "${Scala210}" """
)

libraryDependencies := {
  val libs = libraryDependencies.value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) =>
      libs
    case Some((2, 10)) =>
      // https://github.com/sbt/sbt/blob/v1.1.0/scripted/plugin/src/main/scala/sbt/ScriptedPlugin.scala#L43-L54
      libs.filterNot(_.organization == "org.scala-sbt") ++ Seq(
        "org.scala-sbt" % "scripted-sbt" % sbt013 % ScriptedConf,
        "org.scala-sbt" % "sbt-launch" % sbt013 % ScriptedLaunchConf
      )
    case _ =>
      libs.filterNot(_.organization == "org.scala-sbt")
  }
}

resolvers ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) =>
      // for sbt 0.13 scripted test
      Seq(
        Resolver.url("typesafe ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases/"))(
          Resolver.defaultIvyPatterns))
    case _ =>
      Nil
  }
}

scriptedBufferLog := false

scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(
  a => Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith)
)

scriptedLaunchOpts += {
  s"-Dprotoc-lint-version=${version.value}"
}

description := "protobuf linter"

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

organization := "io.github.scalapb-json"

name := UpdateReadme.projectName

ReleasePlugin.extraReleaseCommands

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value
  else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value)
    sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

unmanagedResources in Compile += (baseDirectory in LocalRootProject).value / "LICENSE.txt"

commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask)

releaseTagName := tagName.value

releaseCrossBuild := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  UpdateReadme.updateReadmeProcess,
  tagRelease,
  ReleaseStep(
    action = { state =>
      val extracted = Project extract state
      extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
    },
    enableCrossBuild = true
  ),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  UpdateReadme.updateReadmeProcess,
  pushChanges
)

scalacOptions in (Compile, doc) ++= {
  val t = tagOrHash.value
  Seq(
    "-sourcepath",
    (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url",
    s"https://github.com/scalapb-json/protoc-lint/tree/${t}€{FILE_PATH}.scala"
  )
}

pomExtra in Global := {
  <url>https://github.com/scalapb-json/protoc-lint</url>
  <scm>
    <connection>scm:git:github.com/scalapb-json/protoc-lint.git</connection>
    <developerConnection>scm:git:git@github.com:scalapb-json/protoc-lint.git</developerConnection>
    <url>https://github.com/scalapb-json/protoc-lint.git</url>
    <tag>{tagOrHash.value}</tag>
  </scm>
  <developers>
    <developer>
      <id>xuwei-k</id>
      <name>Kenji Yoshida</name>
      <url>https://github.com/xuwei-k</url>
    </developer>
  </developers>
}

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "protoc-bridge" % "0.7.0",
  "com.google.protobuf" % "protobuf-java-util" % "3.5.1",
  ("com.typesafe.play" %% "play-json" % "2.6.8").exclude("org.typelevel", "macro-compat_" + scalaBinaryVersion.value)
)

scalaVersion := Scala212

crossScalaVersions := Seq(Scala210, "2.11.12", Scala212)

val unusedWarnings = Seq("-Ywarn-unused", "-Ywarn-unused-import")

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Xlint",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Yno-adapted-args"
)

scalacOptions ++= PartialFunction
  .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, v)) if v >= 11 => unusedWarnings
  }
  .toList
  .flatten

Seq(Compile, Test).flatMap(c => scalacOptions in (c, console) --= unusedWarnings)
