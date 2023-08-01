@file:Suppress("RedundantVisibilityModifier", "unused", "UNUSED_PARAMETER")

package elide.proto.impl.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import elide.proto.api.data.DataContainer as IDataContainer
import elide.data.DataContainer

/** TBD. */
public class FlatDataContainer private constructor (private val container: DataContainer) : IDataContainer<
  FlatDataContainer,
  FlatDataContainer.Builder,
  FlatDataFingerprint,
  FlatDataFingerprint.Builder,
  FlatHashAlgorithm,
  FlatEncoding
> {
  /** TBD. */
  public class Builder : IDataContainer.IBuilder<
    FlatDataContainer,
    FlatDataFingerprint,
    FlatDataFingerprint.Builder,
    FlatHashAlgorithm,
    FlatEncoding,
    Builder,
  > {
    /** @inheritDoc */
    override var data: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var encoding: FlatEncoding
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
    override fun build(): FlatDataContainer {
      TODO("Not yet implemented")
    }
  }

  /** Factory for protocol buffer-backed data containers. */
  public companion object Factory : IDataContainer.Factory<
    FlatDataContainer,
    Builder,
    FlatDataFingerprint,
    FlatDataFingerprint.Builder,
    FlatHashAlgorithm,
    FlatEncoding,
  > {
    /** @inheritDoc */
    override fun empty(): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun copy(model: FlatDataContainer): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun defaultInstance(): FlatDataContainer = TODO("not yet implemented")

    /** @inheritDoc */
    override fun create(encoding: FlatEncoding, data: ByteArray): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(data: ByteArray): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(base64: Base64Data): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(hex: HexData): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(data: String): FlatDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(op: context(Builder) () -> Unit): FlatDataContainer {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun factory(): Factory {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun encoding(): FlatEncoding? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun fingerprint(): FlatDataFingerprint? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun mutate(op: context(Builder) () -> Unit): FlatDataContainer {
    TODO("Not yet implemented")
  }
}
