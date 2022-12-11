@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

/**
 *
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
     *
     */
    public val DEFAULTS: ProtoBufGeneratorOptions = ProtoBufGeneratorOptions()
  }
}
