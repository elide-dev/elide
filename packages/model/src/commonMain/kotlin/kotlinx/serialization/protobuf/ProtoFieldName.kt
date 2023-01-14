package kotlinx.serialization.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Specifies an explicit name to use for a protocol buffer field.
 *
 * See [https://developers.google.com/protocol-buffers/docs/style#message_and_field_names]
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class ProtoFieldName(
  val name: String,
)
