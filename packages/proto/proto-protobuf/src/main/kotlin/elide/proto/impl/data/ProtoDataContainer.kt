/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("RedundantVisibilityModifier", "unused", "UNUSED_PARAMETER")

package elide.proto.impl.data

import tools.elide.crypto.HashAlgorithm
import tools.elide.data.DataContainer
import tools.elide.data.DataContainerOrBuilder
import tools.elide.data.Encoding
import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import elide.proto.api.data.DataContainer as IDataContainer

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
