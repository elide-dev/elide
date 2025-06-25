/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)

package elide.tooling.lockfile

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import kotlinx.io.bytestring.encode
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import elide.runtime.Logging
import elide.tooling.lockfile.ElideLockfile.*

// Label/tag for this version.
private const val LOCKFILE_V1 = "v1"

private enum class FingerprintExpressionMode {
  STRING,
  NUMERIC,
}

private val fingerprintMode = FingerprintExpressionMode.NUMERIC

// Implements Elide's lockfile format at version 1.
internal object ElideLockfileV1 : LockfileDefinition<ElideLockfileV1.LockfileV1> {
  private val logging by lazy {
    Logging.of(ElideLockfileV1::class)
  }

  // Implements the lockfile version 1 outer class.
  @Serializable
  @SerialName(LOCKFILE_V1)
  internal data class LockfileV1 internal constructor (
    override val version: LockfileVersionV1,
    override val fingerprint: Fingerprint,
    override val stanzas: Set<Stanza>,
  ) : ElideLockfile

  // Implements version info for lockfile version 1.
  @Serializable(with = LockfileVersionV1Codec::class) internal object LockfileVersionV1 : Version {
    override val label: String get() = LOCKFILE_V1
    override val ordinal: UInt get() = 1u
  }

  // Implements the lockfile version 1 codec.
  internal object LockfileVersionV1Codec : KSerializer<LockfileVersionV1> {
    override val descriptor: SerialDescriptor get() = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: LockfileVersionV1) {
      encoder.encodeString(value.label)
    }

    override fun deserialize(decoder: Decoder): LockfileVersionV1 {
      val vsn = decoder.decodeString()
      return when (vsn) {
        LOCKFILE_V1 -> LockfileVersionV1
        else -> error("Unsupported lockfile version: $vsn")
      }
    }
  }

  private val fingerprintDescriptor by lazy {
    buildClassSerialDescriptor("fingerprint") {
      when (fingerprintMode) {
        FingerprintExpressionMode.STRING -> element("data", String.serializer().descriptor)
        FingerprintExpressionMode.NUMERIC -> element("hash", Int.serializer().descriptor)
      }
    }
  }

  object FingerprintCodecSerializer: SerializationStrategy<Fingerprint> {
    override val descriptor: SerialDescriptor get() = fingerprintDescriptor

    override fun serialize(encoder: Encoder, value: Fingerprint) {
      when (fingerprintMode) {
        FingerprintExpressionMode.STRING -> encoder.encodeStructure(fingerprintDescriptor) {
          encodeStringElement(descriptor, 0, Base64.encode(value.asBytes()))
        }
        FingerprintExpressionMode.NUMERIC -> encoder.encodeStructure(fingerprintDescriptor) {
          encodeIntElement(descriptor, 0, value.asBytes().let { subject ->
            MessageDigest.getInstance("SHA-1").let { digester ->
              digester.update(subject.toByteArray())
              Base64.encode(digester.digest()).hashCode()
            }
          })
        }
      }
    }
  }

  object FingerprintCodecDeserializer: DeserializationStrategy<Fingerprint> {
    override val descriptor: SerialDescriptor get() = fingerprintDescriptor

    override fun deserialize(decoder: Decoder): Fingerprint {
      return Fingerprint.RawFingerprint.of(
        decoder.decodeStructure(fingerprintDescriptor) {
          when (fingerprintMode) {
            FingerprintExpressionMode.STRING -> decodeStringElement(descriptor, 0).let { fingerprint ->
              Base64.decode(fingerprint)
            }
            FingerprintExpressionMode.NUMERIC -> decodeIntElement(descriptor, 0).let { fingerprint ->
              ByteArray(4) { idx ->
                (fingerprint shr (idx * 8)).toByte()
              }
            }
          }
        }
      )
    }
  }

  // Create a JSON format instance.
  private fun jsonFormat(): Json = Json {
    prettyPrint = true
    decodeEnumsCaseInsensitive = true
    isLenient = true
    ignoreUnknownKeys = true
  }

  // Create a protocol-buffers format instance.
  private fun protoFormat(): ProtoBuf = ProtoBuf {
    encodeDefaults = false
  }

  @Suppress("TooGenericExceptionCaught")
  override fun readFrom(format: Format, stream: InputStream): LockfileV1 {
    return stream.use {
      try {
        when (format) {
          Format.JSON -> {
            jsonFormat().decodeFromString(stream.bufferedReader().use { it.readText() })
          }
          Format.BINARY -> {
            protoFormat().decodeFromByteArray(LockfileV1.serializer(), stream.readAllBytes())
          }
          else -> error("Unsupported format: $format")
        }
      } catch (err: Throwable) {
        logging.error(
          "Failed to read lockfile from stream: ${err.message}",
          err,
        )
        throw LockfileException(
          message = "Failed to read lockfile from stream",
          cause = err,
        )
      }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  override fun writeTo(format: Format, lockfile: ElideLockfile, stream: OutputStream) {
    require(lockfile is LockfileV1) {
      "Expected instance of V1 lockfile version, got ${lockfile::class}"
    }
    stream.use {
      try {
        when (format) {
          Format.JSON -> {
            stream.write(jsonFormat().encodeToString(LockfileV1.serializer(), lockfile).encodeToByteArray())
          }
          Format.BINARY -> {
            stream.write(protoFormat().encodeToByteArray(LockfileV1.serializer(), lockfile))
          }
          else -> error("Unsupported format: $format")
        }
      } catch (err: Throwable) {
        logging.error(
          "Failed to write lockfile to stream: ${err.message}",
          err,
        )
        throw LockfileException(
          message = "Failed to write lockfile to stream",
          cause = err,
        )
      }
    }
  }
}
