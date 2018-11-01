scalaVersion := "2.12.7"

PB.targets in Compile := Seq(
  PB.gens.java -> (sourceManaged in Compile).value,
  protoc_lint.ProtocLint() -> (sourceManaged in Compile).value
)

TaskKey[Unit]("changeProto1") := {
  IO.write(
    file = file("src/main/protobuf/bar.proto"),
    content = """syntax = "proto3";

package com.example;

message bad_name_proto_message {
}
"""
  )
}

TaskKey[Unit]("changeProto2") := {
  IO.delete(file("src/main/protobuf"))
  IO.write(
    file = file("src/main/protobuf/foo.proto"),
    content = """syntax = "proto3";

package com.example;

message Foo {
}
"""
  )

}
