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
