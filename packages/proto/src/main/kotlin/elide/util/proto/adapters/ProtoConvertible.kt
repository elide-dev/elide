package elide.util.proto.adapters

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import kotlin.reflect.full.companionObjectInstance

/**
 *
 */
interface ProtoConvertible<M: Message> : ProtoSchemaConvertible {
  /**
   *
   */
  fun toMessage(): M

  /** @inheritDoc */
  @Suppress("UNCHECKED_CAST")
  override fun toDescriptor(): Descriptors.Descriptor = (this::class.companionObjectInstance as ProtoModel<M>)
    .toDescriptor()
}
