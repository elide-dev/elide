package elide.util.proto.adapters

import com.google.protobuf.Descriptors
import com.google.protobuf.Message

/**
 *
 */
interface ProtoModel<M: Message> : ProtoSchemaConvertible {
  /**
   *
   */
  fun fromMessage(message: M): ProtoConvertible<M>

  /**
   *
   */
  fun defaultMessageInstance(): M

  /** @inheritDoc */
  override fun toDescriptor(): Descriptors.Descriptor = defaultMessageInstance().descriptorForType
}
