package elide.model


/** Describes the expected interface for wire messages, usually implemented via Protocol Buffers on a given platform. */
@Suppress("MemberVisibilityCanBePrivate") actual open class WireMessage {
  /** Message which is wrapped by this wire message. */
  internal lateinit var message: com.google.protobuf.Message

  /**
   * Serialize this [WireMessage] instance into a raw [ByteArray], which is suitable for sending over the wire; formats
   * expressed via this interface must keep schema in sync on both sides.
   *
   * Binary serialization depends on platform but is typically implemented via Protocol Buffer messages. For schemaless
   * serialization, use Proto-JSON.
   *
   * @return Raw bytes of this message, in serialized form.
   */
  actual open fun toSerializedBytes(): ByteArray {
    return this.message.toByteArray()
  }

  /**
   * Return this [WireMessage] as a debug-friendly [String] representation, which emits property values and other info
   * descriptive to the current [WireMessage] instance.
   *
   * @return String-formatted [WireMessage] instance.
   */
  actual open fun toSerializedString(): String {
    return this.message.toString()
  }

  /** @return Underlying [com.google.protobuf.Message] for this [WireMessage]. */
  fun getProto(): com.google.protobuf.Message {
    return this.message
  }
}


/** Describes the expected interface for model objects which are reliably serializable into [WireMessage] instances. */
actual interface AppModel<M: WireMessage> {
  /**
   * Translate the current [AppModel] into an equivalent [WireMessage] instance [M].
   *
   * @return Message instance corresponding to this model.
   */
  actual fun toMessage(): M
}
