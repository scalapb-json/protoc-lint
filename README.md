# protoc-lint

[![Build Status](https://travis-ci.org/scalapb-json/protoc-lint.svg?branch=master)](https://travis-ci.org/scalapb-json/protoc-lint)
[![scaladoc](https://javadoc-badge.appspot.com/io.github.scalapb-json/protoc-lint_2.12.svg?label=scaladoc)](https://javadoc-badge.appspot.com/io.github.scalapb-json/protoc-lint_2.12/protoc_lint/index.html?javadocio=true)

protobuf linter for <https://github.com/thesamet/sbt-protoc>


<https://developers.google.com/protocol-buffers/docs/style>


## sbt settings

### `project/plugins.sbt`

```scala
libraryDependencies += "io.github.scalapb-json" %% "protoc-lint" % "0.2.1"
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.17")
```

or

```scala
// shaded version. you can avoid dependency conflict. only sbt Scala 2.12, sbt 1.x
libraryDependencies += "io.github.scalapb-json" %% "protoc-lint-shaded" % "0.2.1"
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.17")
```


### `build.sbt`

```scala
PB.targets in Compile := Seq(
  protoc_lint.ProtocLint() -> (sourceManaged in Compile).value,
  // and add another generator settings (e.g. java, scalapb)
  // see https://github.com/thesamet/sbt-protoc
)
```
