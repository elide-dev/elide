@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

/**
 * # Protobuf Generator: Options
 *
 * Specifies the set of options which apply to the protocol buffer source generator. The generator can generate Proto2
 * or Proto3 syntax from regular Kotlin models, with special annotations for controlling the output.
 *
 * @param packageName package name to use for generated models; if `null`, a sensible one will be generated.
 * @param packageOptions top-level package options to apply.
 * @param protoOptions proto options to apply.
 * @param emitWarningComments whether to emit file warnings indicating generated code.
 * @param syntaxVersion protocol buffers syntax version to emit.
 */
public data class ProtoBufGeneratorOptions(
  val packageName: String? = null,
  val packageOptions: Map<String, String>? = null,
  val protoOptions: Collection<ProtoOption<*>> = emptyList(),
  val emitWarningComments: Boolean = false,
  val syntaxVersion: ProtoBufSyntaxVersion = ProtoBufSyntaxVersion.PROTO2,
) {
  public companion object {
    /**
     * Default protocol buffer generator options.
     */
    public val DEFAULTS: ProtoBufGeneratorOptions = ProtoBufGeneratorOptions()
  }
}
