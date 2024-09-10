package protoc_lint

import com.google.protobuf.DescriptorProtos._
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import argonaut._

sealed abstract class LintError extends Product with Serializable {
  final def toJson: Json = implicitly[EncodeJson[LintError]].encode(this)
  final override def toString = toJson.toString
}

object LintError {
  final case class EnumNameCapitalsWithUnderscores(enumValue: EnumValueDescriptorProto) extends LintError
  final case class ServiceNameCamel(service: ServiceDescriptorProto) extends LintError
  final case class ServiceMethodNameCamel(service: MethodDescriptorProto) extends LintError
  final case class MessageNameCamel(service: DescriptorProto) extends LintError
  final case class FieldNameUnderscoreSeparated(field: FieldDescriptorProto) extends LintError

  implicit class GeneratedMessageOps[A <: MessageOrBuilder](private val self: A) extends AnyVal {
    def toJsonString: String =
      JsonFormat.printer.print(self)
    def toJson: Json =
      JsonParser.parse(toJsonString).fold(sys.error, identity)
  }

  implicit val encodeJson: EncodeJson[LintError] = new EncodeJson[LintError] {
    override def encode(o: LintError) = {
      def build(tpe: String, src: Json) =
        Json.jObjectFields(
          "error_type" -> Json.jString(tpe),
          "source" -> src
        )

      o match {
        case EnumNameCapitalsWithUnderscores(enumValue) =>
          build(EnumNameCapitalsWithUnderscores.toString, enumValue.toJson)
        case ServiceNameCamel(service) =>
          build(ServiceNameCamel.toString, service.toJson)
        case ServiceMethodNameCamel(service) =>
          build(ServiceMethodNameCamel.toString, service.toJson)
        case MessageNameCamel(message) =>
          build(MessageNameCamel.toString, message.toJson)
        case FieldNameUnderscoreSeparated(field) =>
          build(FieldNameUnderscoreSeparated.toString, field.toJson)
      }
    }
  }
}
