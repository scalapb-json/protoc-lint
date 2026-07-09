libraryDependencies += {
  val v = (pluginCrossBuild / sbtBinaryVersion).value match {
    case "1.0" =>
      "1.0.8"
    case "2" =>
      "1.1.0-RC1"
  }
  Defaults.sbtPluginExtra(
    "com.thesamet" % "sbt-protoc" % v,
    (pluginCrossBuild / sbtBinaryVersion).value,
    (update / scalaBinaryVersion).value
  )
}

libraryDependencies += "io.github.scalapb-json" %% System.getProperty("protoc-lint-artifact-id") % System.getProperty(
  "protoc-lint-version"
)
resolvers += Resolver.mavenLocal
