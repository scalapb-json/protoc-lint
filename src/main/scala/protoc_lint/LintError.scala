package protoc_lint

import com.google.protobuf.DescriptorProtos._
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import play.api.libs.json._

sealed abstract class LintError extends Product with Serializable {
  final def toJson: JsValue = implicitly[Writes[LintError]].writes(this)
  final override def toString = toJson.toString
}

object LintError {
  final case class EnumNameCapitalsWithUnderscores(enumValue: EnumValueDescriptorProto) extends LintError
  final case class ServiceNameCamel(service: ServiceDescriptorProto) extends LintError
  final case class ServiceMethodNameCamel(service: MethodDescriptorProto) extends LintError
  final case class MessageNameCamel(service: DescriptorProto) extends LintError
  final case class FieldNameUnderscoreSeparated(field: FieldDescriptorProto) extends LintError

  implicit class GeneratedMessageOps[A <: MessageOrBuilder](val self: A) extends AnyVal {
    def toJsonString: String =
      JsonFormat.printer.print(self)
    def toPlayJson: JsValue =
      Json.parse(toJsonString)
  }

  implicit val writes: Writes[LintError] = new Writes[LintError] {
    def writes(o: LintError): JsValue = {
      def build(tpe: String, src: JsValue) =
        Json.obj(
          "error_type" -> tpe,
          "source" -> src
        )

      o match {
        case EnumNameCapitalsWithUnderscores(enumValue) =>
          build(EnumNameCapitalsWithUnderscores.toString, enumValue.toPlayJson)
        case ServiceNameCamel(service) =>
          build(ServiceNameCamel.toString, service.toPlayJson)
        case ServiceMethodNameCamel(service) =>
          build(ServiceMethodNameCamel.toString, service.toPlayJson)
        case MessageNameCamel(message) =>
          build(MessageNameCamel.toString, message.toPlayJson)
        case FieldNameUnderscoreSeparated(field) =>
          build(FieldNameUnderscoreSeparated.toString, field.toPlayJson)
      }
    }
  }
}
