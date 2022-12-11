@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

/**
 *
 */
public enum class ProtoBufSyntaxVersion constructor (internal val symbol: String) {
  /**
   *
   */
  PROTO2("proto2"),

  /**
   *
   */
  PROTO3("proto3");
}
