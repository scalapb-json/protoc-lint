# protoc-lint

[![Build Status](https://travis-ci.org/scalapb-json/protoc-lint.svg?branch=master)](https://travis-ci.org/scalapb-json/protoc-lint)

protobuf linter for <https://github.com/thesamet/sbt-protoc>


<https://developers.google.com/protocol-buffers/docs/style>


## sbt settings

- `project/plugins.sbt`

```scala
libraryDependencies += "io.github.scalapb-json" %% "protoc-lint" % "0.1.0"
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.13")
```


- `build.sbt`

```scala
PB.targets in Compile := Seq(
  protoc_lint.ProtocLint() -> (sourceManaged in Compile).value,
  // and add another generator settings (e.g. java, scalapb)
  // see https://github.com/thesamet/sbt-protoc
)
```
