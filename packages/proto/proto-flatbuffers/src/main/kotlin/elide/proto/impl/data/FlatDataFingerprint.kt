@file:Suppress("RedundantVisibilityModifier", "unused", "UNUSED_PARAMETER")

package elide.proto.impl.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import elide.data.DataFingerprint
import elide.proto.api.data.DataFingerprint as IDataFingerprint

/** TBD. */
public class FlatDataFingerprint private constructor (private val fingerprint: DataFingerprint) : IDataFingerprint<
  FlatDataFingerprint,
  FlatDataFingerprint.Builder,
  FlatHashAlgorithm,
  FlatEncoding,
> {
  /** TBD. */
  public class Builder : IDataFingerprint.IBuilder<
    FlatDataFingerprint,
    FlatHashAlgorithm,
    FlatEncoding,
    Builder,
  > {
    /** @inheritDoc */
    override var fingerprint: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var algorithm: FlatHashAlgorithm
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var encoding: FlatEncoding
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override fun setFingerprint(data: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setFingerprint(data: ByteArray, withAlgorith: FlatHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setFingerprint(
      value: ByteArray,
      withAlgorith: FlatHashAlgorithm,
      withEncoding: FlatEncoding
    ): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setAlgorithm(value: FlatHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setEncoding(value: FlatEncoding): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun build(): FlatDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  /** Factory for protocol buffer-backed data containers. */
  public companion object Factory : IDataFingerprint.Factory<
    FlatDataFingerprint,
    Builder,
    FlatHashAlgorithm,
    FlatEncoding,
  > {
    /** @inheritDoc */
    override fun empty(): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun copy(model: FlatDataFingerprint): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun defaultInstance(): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(op: context(Builder) () -> Unit): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: FlatHashAlgorithm, data: ByteArray): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: FlatHashAlgorithm, base64: Base64Data): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: FlatHashAlgorithm, hex: HexData): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: FlatHashAlgorithm, data: String): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: FlatHashAlgorithm, data: String, encoding: FlatEncoding): FlatDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun factory(): Factory = Factory

  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  override fun encoding(): FlatEncoding {
    TODO("Not yet implemented")
  }

  override fun algorithm(): HashAlgorithm {
    TODO("Not yet implemented")
  }
}
