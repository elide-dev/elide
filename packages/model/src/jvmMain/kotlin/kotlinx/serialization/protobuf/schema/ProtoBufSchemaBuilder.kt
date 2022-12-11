@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 *
 */
public interface ProtoBufSchemaBuilder {
  /**
   *
   */
  public fun generateSchemaText(
    descriptors: List<SerialDescriptor>,
    options: ProtoBufGeneratorOptions = ProtoBufGeneratorOptions.DEFAULTS,
  ): String
}
