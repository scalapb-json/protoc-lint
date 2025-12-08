scalaVersion := "2.12.21"

PB.protocVersion := "4.32.1"

(Compile / PB.targets) ++= Seq[protocbridge.Target](
  PB.gens.java(PB.protocVersion.value) -> (Compile / sourceManaged).value,
  protoc_lint.ProtocLint() -> (Compile / sourceManaged).value
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
  optional string my_field = 1;
}
"""
  )

}
