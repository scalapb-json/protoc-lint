# protoc-lint

[![scaladoc](https://javadoc.io/badge2/io.github.scalapb-json/protoc-lint_2.13/javadoc.svg)](https://javadoc.io/doc/io.github.scalapb-json/protoc-lint_2.13)
protobuf linter for <https://github.com/thesamet/sbt-protoc>


<https://developers.google.com/protocol-buffers/docs/style>


## sbt settings

### `project/plugins.sbt`

```scala
libraryDependencies += "io.github.scalapb-json" %% "protoc-lint" % "0.6.0"
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")
```

or

```scala
// shaded version. you can avoid dependency conflict. only sbt Scala 2.12, sbt 1.x
libraryDependencies += "io.github.scalapb-json" %% "protoc-lint-shaded" % "0.6.0"
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")
```


### `build.sbt`

```scala
Compile / PB.targets := Seq(
  protoc_lint.ProtocLint() -> (Compile / sourceManaged).value,
  // and add another generator settings (e.g. java, scalapb)
  // see https://github.com/thesamet/sbt-protoc
)
```
