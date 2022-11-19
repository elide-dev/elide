package elide.util.proto.adapters

import com.google.protobuf.Descriptors.Descriptor

/**
 * Schema converter which is capable of converting universal Elide model schemas into Protocol Buffer definitions, which
 * can then be expressed via APIs or database tables.
 *
 * Utilities are present on this class to generate Protocol Buffer message types, enumerations, references, fields, and
 * more. The developer may also elect to map a static [Descriptor] if they so choose. Adopting this class either
 * involves implementing via the model's companion object, or transitively by way of the universal model interface.
 */
interface ProtoSchemaConvertible {
  /**
   * Produce a Protocol Buffer [Descriptor] for the associated model definition; the descriptor may be produced on-the-
   * fly or mapped statically.
   *
   * @return Protocol Buffer descriptor describing this model.
   */
  fun toDescriptor(): Descriptor
}
