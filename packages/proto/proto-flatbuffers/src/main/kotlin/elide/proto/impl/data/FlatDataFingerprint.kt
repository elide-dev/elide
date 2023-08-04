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
    override var fingerprint: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    override var algorithm: FlatHashAlgorithm
      get() = TODO("Not yet implemented")
      set(value) {}

    override var encoding: FlatEncoding
      get() = TODO("Not yet implemented")
      set(value) {}

    override fun setFingerprint(data: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    override fun setFingerprint(data: ByteArray, withAlgorith: FlatHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    override fun setFingerprint(
      value: ByteArray,
      withAlgorith: FlatHashAlgorithm,
      withEncoding: FlatEncoding
    ): Builder {
      TODO("Not yet implemented")
    }

    override fun setAlgorithm(value: FlatHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    override fun setEncoding(value: FlatEncoding): Builder {
      TODO("Not yet implemented")
    }

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
    override fun empty(): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun copy(model: FlatDataFingerprint): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun defaultInstance(): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    override fun create(op: context(Builder) () -> Unit): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: FlatHashAlgorithm, data: ByteArray): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: FlatHashAlgorithm, base64: Base64Data): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: FlatHashAlgorithm, hex: HexData): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: FlatHashAlgorithm, data: String): FlatDataFingerprint {
      TODO("Not yet implemented")
    }

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
