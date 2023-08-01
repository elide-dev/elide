@file:Suppress("RedundantVisibilityModifier", "unused", "UNUSED_PARAMETER")

package elide.proto.impl.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import tools.elide.crypto.HashAlgorithm
import elide.proto.api.data.DataContainer as IDataContainer
import tools.elide.data.DataContainer
import tools.elide.data.Encoding
import tools.elide.data.DataContainerOrBuilder

/** TBD. */
public class ProtoDataContainer private constructor (private val container: DataContainer) : IDataContainer<
  ProtoDataContainer,
  ProtoDataContainer.Builder,
  ProtoDataFingerprint,
  ProtoDataFingerprint.Builder,
  HashAlgorithm,
  Encoding,
>, DataContainerOrBuilder by container {
  /** TBD. */
  public class Builder : IDataContainer.IBuilder<
    ProtoDataContainer,
    ProtoDataFingerprint,
    ProtoDataFingerprint.Builder,
    HashAlgorithm,
    Encoding,
    Builder,
  > {
    /** @inheritDoc */
    override var data: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    /** @inheritDoc */
    override var encoding: Encoding
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
    override fun build(): ProtoDataContainer {
      TODO("Not yet implemented")
    }
  }

  /** Factory for protocol buffer-backed data containers. */
  public companion object Factory : IDataContainer.Factory<
    ProtoDataContainer,
    Builder,
    ProtoDataFingerprint,
    ProtoDataFingerprint.Builder,
    HashAlgorithm,
    Encoding,
  > {
    /** Default singleton (empty) instance. */
    @JvmStatic private val DEFAULT_INSTANCE: ProtoDataContainer = ProtoDataContainer(
      DataContainer.getDefaultInstance()
    )

    /** @inheritDoc */
    override fun empty(): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun copy(model: ProtoDataContainer): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun defaultInstance(): ProtoDataContainer = DEFAULT_INSTANCE

    /** @inheritDoc */
    override fun create(encoding: Encoding, data: ByteArray): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(data: ByteArray): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(base64: Base64Data): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(hex: HexData): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(data: String): ProtoDataContainer {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    /** @inheritDoc */
    override fun create(op: context(Builder) () -> Unit): ProtoDataContainer {
      TODO("Not yet implemented")
    }
  }

  /** @inheritDoc */
  override fun factory() = Factory

  /** @inheritDoc */
  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun encoding(): Encoding? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun fingerprint(): ProtoDataFingerprint? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun mutate(op: context(Builder) () -> Unit): ProtoDataContainer {
    TODO("Not yet implemented")
  }
}
