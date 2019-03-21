package protoc_lint

import protocbridge.ProtocCodeGenerator
import com.google.protobuf.DescriptorProtos.{EnumValueDescriptorProto, FieldDescriptorProto, FileDescriptorProto}
import com.google.protobuf.compiler.PluginProtos._
import argonaut._
import scala.collection.JavaConverters._

case class ProtocLint(exclude: LintError => Boolean = _ => false) extends ProtocCodeGenerator {

  override def run(req: Array[Byte]): Array[Byte] =
    run0(CodeGeneratorRequest.parseFrom(req)).toByteArray

  private[this] def run0(req: CodeGeneratorRequest): CodeGeneratorResponse = {
    val errors = lint(req)
    if (errors.isEmpty) {
      println("success lint")
      CodeGeneratorResponse.newBuilder().build()
    } else {
      val (excluded, realErrors) = errors.partition(exclude)

      println(
        s"found ${errors.size} lint errors. excluded ${excluded.size} errors. realErrors count = ${realErrors.size}"
      )
      println(Json.jArray(excluded.map(_.toJson)).spaces2)

      if (realErrors.isEmpty) {
        CodeGeneratorResponse.newBuilder().build()
      } else {
        val errorJson = Json.jArray(realErrors.map(_.toJson)).spaces2
        CodeGeneratorResponse.newBuilder().setError(errorJson).build()
      }
    }
  }

  private[this] val Camel = "([A-Z][a-z0-9]+)+"
  private[this] val CapitalsWithUnderscores = "([A-Z]|[0-9]|_)+"
  private[this] val UnderscoreSeparated = "[a-z0-9_]+"

  private[this] def lint(req: CodeGeneratorRequest): List[LintError] = {
    val toGenerate = req.getFileToGenerateList.asScala.toSet
    val files = req.getProtoFileList.asScala.filter { p =>
      toGenerate.contains(p.getName)
    }
    lint0(files)
  }

  private[this] def checkEnum(enumValue: EnumValueDescriptorProto): Option[LintError] = {
    val name = enumValue.getName
    if (name.matches(CapitalsWithUnderscores)) {
      Option.empty[LintError]
    } else {
      Some(LintError.EnumNameCapitalsWithUnderscores(enumValue))
    }
  }

  private[this] def checkFields(fields: collection.Seq[FieldDescriptorProto]): List[LintError] =
    fields.flatMap { field =>
      val name = field.getName
      if (name.matches(UnderscoreSeparated)) {
        Nil
      } else {
        List(LintError.FieldNameUnderscoreSeparated(field))
      }
    }.toList

  private[this] def lint0(files: collection.Seq[FileDescriptorProto]): List[LintError] = {
    files.flatMap { f =>
      List(
        f.getMessageTypeList.asScala.flatMap { message =>
          List(
            message.getNestedTypeList.asScala.flatMap { m =>
              checkFields(m.getFieldList.asScala)
            },
            message.getEnumTypeList.asScala.flatMap(
              _.getValueList.asScala.flatMap(checkEnum)
            ),
            checkFields(message.getFieldList.asScala), {
              val name = message.getName
              if (name.matches(Camel)) {
                Nil
              } else {
                List(LintError.MessageNameCamel(message))
              }
            }
          ).flatten
        },
        f.getEnumTypeList.asScala.flatMap(
          _.getValueList.asScala.flatMap(checkEnum)
        ),
        f.getServiceList.asScala.flatMap { s =>
          List(
            {
              val name = s.getName
              if (name.matches(Camel)) {
                Nil
              } else {
                List(LintError.ServiceNameCamel(s))
              }
            },
            s.getMethodList.asScala.flatMap { method =>
              val name = method.getName
              if (name.matches(Camel)) {
                None
              } else {
                Option(LintError.ServiceMethodNameCamel(method))
              }
            }
          ).flatten
        }
      ).flatten
    }.toList
  }
}
