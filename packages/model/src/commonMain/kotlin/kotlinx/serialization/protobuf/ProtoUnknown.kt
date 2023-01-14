@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * # Proto: Unknown Value
 *
 * Marks a value on a protocol-buffers enumeration as the "unknown," or default value, for the enumeration. If no value
 * is specified over the wire, this value will be returned. The ID for this value in the enumeration is always `0`.
 */
@SerialInfo
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoUnknown
