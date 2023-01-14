package kotlinx.serialization.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Specifies that fields on a protocol buffer model should not be re-written according to protobuf camel casing and
 * underscore rules.
 *
 * See [https://developers.google.com/protocol-buffers/docs/style#message_and_field_names]
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class ProtoFieldsPreserve(
  val name: String,
)
