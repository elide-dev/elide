@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 *
 */
@SerialInfo
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
public annotation class ProtoEnum(
  val unknownName: String,
  val aliases: Boolean,
)
