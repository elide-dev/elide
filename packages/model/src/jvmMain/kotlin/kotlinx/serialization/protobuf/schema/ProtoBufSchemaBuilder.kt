@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * # Protobuf: Schema Generator
 *
 * Describes the interface which protocol buffer schema generators adhere to. There is only one method at the time of
 * this writing; [generateSchemaText], which generates proto-schema syntax for a given set of [SerialDescriptor]
 * instances, each for a Kotlin model.
 */
public interface ProtoBufSchemaBuilder {
  /**
   * Generate protocol buffer schema from the provided set of model [descriptors].
   *
   * @param descriptors Descriptors to generate schema for.
   * @param options Options to apply.
   * @return Generated syntax.
   */
  public fun generateSchemaText(
    descriptors: List<SerialDescriptor>,
    options: ProtoBufGeneratorOptions = ProtoBufGeneratorOptions.DEFAULTS,
  ): String
}
