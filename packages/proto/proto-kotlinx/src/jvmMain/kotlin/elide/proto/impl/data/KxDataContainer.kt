@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import elide.data.DataContainer
import elide.proto.api.data.DataContainer as IDataContainer
import elide.proto.api.data.DataFingerprint as IDataFingerprint

/** TBD. */
@Suppress("unused", "UNUSED_PARAMETER")
public class KxDataContainer private constructor (private val container: DataContainer) : IDataContainer<
  KxDataContainer,
  KxDataContainer.Builder,
  KxDataFingerprint,
  KxDataFingerprint.Builder,
  KxHashAlgorithm,
  KxEncoding,
> {
  /**
   * TBD.
   */
  public class Builder : IDataContainer.IBuilder<
    KxDataContainer,
    KxDataFingerprint,
    KxDataFingerprint.Builder,
    KxHashAlgorithm,
    KxEncoding,
    Builder,
  > {
    /** @inheritDoc */
    override var data: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var encoding: KxEncoding
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override fun setData(value: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setData(value: String): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setBase64(value: Base64Data): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setHex(value: HexData): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun build(): KxDataContainer {
      TODO("Not yet implemented")
    }
  }

  /** */
  public companion object Factory : IDataContainer.Factory<
    KxDataContainer,
    Builder,
    KxDataFingerprint,
    KxDataFingerprint.Builder,
    KxHashAlgorithm,
    KxEncoding,
  > {
    /** @inheritDoc */
    override fun empty(): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun copy(model: KxDataContainer): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun defaultInstance(): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(op: Builder.() -> Unit): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(encoding: KxEncoding, data: ByteArray): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(data: ByteArray): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(base64: Base64Data): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(hex: HexData): KxDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(data: String): KxDataContainer {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun factory() = Factory

  /** @inheritDoc */
  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun encoding(): KxEncoding? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun fingerprint(): IDataFingerprint<*, *, *, *>? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun mutate(op: context(Builder) () -> Unit): KxDataContainer {
    TODO("Not yet implemented")
  }
}
