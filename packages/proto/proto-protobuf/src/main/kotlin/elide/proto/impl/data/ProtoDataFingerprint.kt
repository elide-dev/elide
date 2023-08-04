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
import tools.elide.data.DataFingerprint
import tools.elide.data.DataFingerprintOrBuilder
import tools.elide.data.Encoding
import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import tools.elide.data.DataFingerprint.Builder as ProtoBuilder
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
    override var fingerprint: ByteArray
      get() = TODO("Not yet implemented")
      set(value) {}

    override var algorithm: HashAlgorithm
      get() = TODO("Not yet implemented")
      set(value) {}

    override var encoding: Encoding
      get() = TODO("Not yet implemented")
      set(value) {}

    override fun setFingerprint(data: ByteArray): Builder {
      TODO("Not yet implemented")
    }

    override fun setFingerprint(data: ByteArray, withAlgorith: HashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    override fun setFingerprint(value: ByteArray, withAlgorith: HashAlgorithm, withEncoding: Encoding): Builder {
      TODO("Not yet implemented")
    }

    override fun setAlgorithm(value: HashAlgorithm): Builder {
      TODO("Not yet implemented")
    }

    override fun setEncoding(value: Encoding): Builder {
      TODO("Not yet implemented")
    }

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
    override fun empty(): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun copy(model: ProtoDataFingerprint): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun defaultInstance(): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun builder(): Builder {
      TODO("Not yet implemented")
    }

    override fun create(op: Builder.() -> Unit): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: HashAlgorithm, data: ByteArray): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: HashAlgorithm, base64: Base64Data): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: HashAlgorithm, hex: HexData): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: HashAlgorithm, data: String): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }

    override fun create(algorithm: HashAlgorithm, data: String, encoding: Encoding): ProtoDataFingerprint {
      TODO("Not yet implemented")
    }
  }

  override fun factory(): Factory = Factory

  override fun toBuilder(): Builder {
    TODO("Not yet implemented")
  }

  override fun bytes(): ByteArray {
    TODO("Not yet implemented")
  }

  override fun encoding(): Encoding {
    TODO("Not yet implemented")
  }

  override fun algorithm(): HashAlgorithm {
    TODO("Not yet implemented")
  }
}
