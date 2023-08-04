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
    override var fingerprint: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    override var algorithm: KxHashAlgorithm
      get() = TODO("Not yet implemented")
      set(value) {}

    override var encoding: KxEncoding
      get() = TODO("Not yet implemented")
      set(value) {}

    override fun setFingerprint(data: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    override fun setFingerprint(data: ByteArray, withAlgorith: KxHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    override fun setFingerprint(value: ByteArray, withAlgorith: KxHashAlgorithm, withEncoding: KxEncoding): Builder {
      TODO("Not yet implemented")
    }

    override fun setAlgorithm(value: KxHashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    override fun setEncoding(value: KxEncoding): Builder {
      TODO("Not yet implemented")
    }

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
    override fun empty(): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun copy(model: KxDataFingerprint): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun defaultInstance(): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    override fun create(op: Builder.() -> Unit): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: KxHashAlgorithm, data: ByteArray): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: KxHashAlgorithm, base64: Base64Data): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: KxHashAlgorithm, hex: HexData): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: KxHashAlgorithm, data: String): KxDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: KxHashAlgorithm, data: String, encoding: KxEncoding): KxDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  override fun factory() = Factory

  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  override fun encoding(): KxEncoding {
    TODO("Not yet implemented")
  }

  override fun algorithm(): KxHashAlgorithm {
    TODO("Not yet implemented")
  }
}
