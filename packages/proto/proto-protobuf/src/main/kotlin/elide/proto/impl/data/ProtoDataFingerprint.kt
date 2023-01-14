@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.DataFingerprint
import tools.elide.data.DataFingerprint.Builder as ProtoBuilder
import tools.elide.data.DataFingerprintOrBuilder
import tools.elide.data.Encoding
import elide.proto.api.data.DataFingerprint as IDataFingerprint

/** Implements a universal data fingerprint, backed by a protocol buffer record. */
public class ProtoDataFingerprint private constructor (private val fingerprint: DataFingerprint) :
  IDataFingerprint<ProtoDataFingerprint, ProtoDataFingerprint.Builder, HashAlgorithm, Encoding>,
  DataFingerprintOrBuilder by fingerprint {
  /** TBD. */
  public class Builder private constructor (private val builder: ProtoBuilder) : IDataFingerprint.IBuilder<
    ProtoDataFingerprint,
    HashAlgorithm,
    Encoding,
    Builder,
  > {
    /** @inheritDoc */
    override var fingerprint: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var algorithm: HashAlgorithm
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var encoding: Encoding
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override fun setFingerprint(data: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setFingerprint(data: ByteArray, withAlgorith: HashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setFingerprint(value: ByteArray, withAlgorith: HashAlgorithm, withEncoding: Encoding): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setAlgorithm(value: HashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun setEncoding(value: Encoding): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun build(): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  /** Factory for protocol buffer-backed data containers. */
  public companion object Factory : IDataFingerprint.Factory<
    ProtoDataFingerprint,
    Builder,
    HashAlgorithm,
    Encoding,
  > {
    /** @inheritDoc */
    override fun empty(): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun copy(model: ProtoDataFingerprint): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun defaultInstance(): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(op: Builder.() -> Unit): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: HashAlgorithm, data: ByteArray): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: HashAlgorithm, base64: Base64Data): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: HashAlgorithm, hex: HexData): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: HashAlgorithm, data: String): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(algorithm: HashAlgorithm, data: String, encoding: Encoding): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun factory(): Factory = Factory

  /** @inheritDoc */
  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun encoding(): Encoding {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun algorithm(): HashAlgorithm {
    TODO("Not yet implemented")
  }
}
