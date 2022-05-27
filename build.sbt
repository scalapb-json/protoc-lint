import sbtrelease.ReleaseStateTransformations._
import scala.collection.JavaConverters._
import java.lang.management.ManagementFactory

val Scala212 = "2.12.15"
val Scala213 = "2.13.8"

Global / onChangedBuildSource := ReloadOnSourceChanges

val unusedWarnings = Seq("-Ywarn-unused")

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value
    else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value)
    sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val scriptedSettings = Seq(
  libraryDependencies := {
    val libs = libraryDependencies.value
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        libs
      case _ =>
        libs.filterNot(_.organization == "org.scala-sbt")
    }
  },
  sbtTestDirectory := file("test"),
  scriptedBufferLog := false,
  scriptedLaunchOpts ++= ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList.filter(a =>
    Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith)
  ),
  scriptedLaunchOpts ++= Seq(
    s"-Dprotoc-lint-version=${version.value}",
    s"-Dprotoc-lint-artifact-id=${name.value}"
  )
)

val cleanLocalMaven = "cleanLocalMaven"

TaskKey[Unit](cleanLocalMaven) := {
  val dir = Path.userHome / s".m2/repository/${organization.value.replace('.', '/')}"
  println("delete " + dir)
  IO.delete(dir)
}

val commonSettings = Def.settings(
  description := "protobuf linter",
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  ReleasePlugin.extraReleaseCommands,
  commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
  releaseTagName := tagName.value,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    UpdateReadme.updateReadmeProcess,
    tagRelease,
    releaseStepCommandAndRemaining("set ThisBuild / useSuperShell := false"),
    releaseStepCommandAndRemaining("+ publishSigned"),
    releaseStepCommandAndRemaining("set ThisBuild / useSuperShell := true"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    UpdateReadme.updateReadmeProcess,
    pushChanges
  ),
  (Compile / doc / scalacOptions) ++= {
    val t = tagOrHash.value
    Seq(
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/scalapb-json/protoc-lint/tree/${t}€{FILE_PATH}.scala"
    )
  },
  (Global / pomExtra) := {
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
  },
  publishLocal := {}, // use local maven in scripted-test
  publishTo := sonatypePublishToBundle.value,
  scalaVersion := Scala212,
  crossScalaVersions := Seq(Scala212),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-Xlint",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions"
  ),
  scalacOptions ++= PartialFunction
    .condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
      case Some((2, v)) if v <= 12 => Seq("-Yno-adapted-args")
    }
    .toList
    .flatten,
  libraryDependencies += "com.thesamet.scalapb" %% "protoc-bridge" % "0.9.5",
  scalacOptions ++= unusedWarnings,
  Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedWarnings)
)

commonSettings

val noPublish = Seq(
  publish / skip := true,
  publishArtifact := false,
  publish := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {}
)

noPublish

commands += Command.command("testAll") {
  List(
    cleanLocalMaven,
    s"project ${shaded.id}",
    "+ publishM2",
    "scripted",
    s"project ${protocLint.id}",
    "+ publishM2",
    "scripted",
    "project /",
    cleanLocalMaven
  ) ::: _
}

val argonautVersion = settingKey[String]("")

val protocLint = Project("protoc-lint", file("protoc-lint"))
  .settings(
    commonSettings,
    crossScalaVersions := Seq(Scala212, Scala213),
    scriptedSettings,
    (Compile / unmanagedResources) += (LocalRootProject / baseDirectory).value / "LICENSE.txt",
    name := UpdateReadme.projectName,
    argonautVersion := "6.3.8",
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java-util" % "3.21.1",
      "io.argonaut" %% "argonaut" % argonautVersion.value
    )
  )
  .enablePlugins(ScriptedPlugin)

val shadeTarget = settingKey[String]("Target to use when shading")

(ThisBuild / shadeTarget) := s"protoc_lint_shaded.v${version.value.replaceAll("[.-]", "_")}.@0"

val shaded = Project("shaded", file("shaded"))
  .settings(
    commonSettings,
    scriptedSettings,
    name := UpdateReadme.shadedName,
    (assembly / assemblyShadeRules) := Seq(
      ShadeRule.rename("com.google.**" -> shadeTarget.value).inAll,
      ShadeRule.rename("play.api.libs.**" -> shadeTarget.value).inAll,
      ShadeRule.rename("argonaut.**" -> shadeTarget.value).inAll
    ),
    (assembly / assemblyExcludedJars) := {
      val toInclude = Seq(
        "gson",
        "guava",
        "argonaut",
        "protobuf-java",
        "protobuf-java-util"
      )

      (assembly / fullClasspath).value.filterNot { c => toInclude.exists(prefix => c.data.getName.startsWith(prefix)) }
    },
    (Compile / packageBin / artifact) := (Compile / assembly / artifact).value,
    addArtifact(Compile / packageBin / artifact, assembly),
    pomPostProcess := { node =>
      import scala.xml.Comment
      import scala.xml.Elem
      import scala.xml.Node
      import scala.xml.transform.RuleTransformer
      import scala.xml.transform.RewriteRule
      new RuleTransformer(new RewriteRule {
        override def transform(node: Node) =
          node match {
            case e: Elem
                if e.label == "dependency" && e.child
                  .exists(child => child.label == "artifactId" && child.text.startsWith(UpdateReadme.projectName)) =>
              Comment(s"scalapb_lint has been removed.")
            case _ =>
              node
          }
      }).transform(node).head
    }
  )
  .dependsOn(protocLint)
  .enablePlugins(ScriptedPlugin)

val root = project.in(file(".")).aggregate(protocLint, shaded)
