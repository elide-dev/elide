@file:Suppress("RedundantVisibilityModifier", "unused", "UNUSED_PARAMETER")

package elide.proto.impl.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import elide.data.DataFingerprint
import elide.proto.api.data.DataFingerprint as IDataFingerprint

/** TBD. */
public class KxDataFingerprint private constructor (private val fingerprint: DataFingerprint) : IDataFingerprint<
  KxDataFingerprint,
  KxDataFingerprint.Builder,
  KxHashAlgorithm,
  KxEncoding,
> {
  /** TBD. */
  public class Builder : IDataFingerprint.IBuilder<
    KxDataFingerprint,
    KxHashAlgorithm,
    KxEncoding,
    Builder,
  > {
    /** @inheritDoc */
    override var fingerprint: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var algorithm: KxHashAlgorithm
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var encoding: KxEncoding
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override fun setFingerprint(data: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setFingerprint(data: ByteArray, withAlgorith: KxHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setFingerprint(value: ByteArray, withAlgorith: KxHashAlgorithm, withEncoding: KxEncoding): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setAlgorithm(value: KxHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setEncoding(value: KxEncoding): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun build(): KxDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  /** TBD. */
  public companion object Factory : IDataFingerprint.Factory<
    KxDataFingerprint,
    Builder,
    KxHashAlgorithm,
    KxEncoding,
  > {
    /** @inheritDoc */
    override fun empty(): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun copy(model: KxDataFingerprint): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun defaultInstance(): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(op: Builder.() -> Unit): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: KxHashAlgorithm, data: ByteArray): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: KxHashAlgorithm, base64: Base64Data): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: KxHashAlgorithm, hex: HexData): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: KxHashAlgorithm, data: String): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: KxHashAlgorithm, data: String, encoding: KxEncoding): KxDataFingerprint {
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
  override fun encoding(): KxEncoding {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun algorithm(): KxHashAlgorithm {
    TODO("Not yet implemented")
  }
}
