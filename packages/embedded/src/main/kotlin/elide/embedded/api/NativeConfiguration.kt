/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.api

import com.google.protobuf.InvalidProtocolBufferException
import tools.elide.call.HostConfiguration
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 *
 */
public class NativeConfiguration private constructor (private val config: DecodedNativeConfiguration) {
  /**
   *
   */
  @JvmRecord private data class DecodedNativeConfiguration (
    val version: String,
    val mode: ProtocolMode,
    private val bytes: ByteBuffer = ByteBuffer.allocate(0),
    private val payload: AtomicReference<HostConfiguration> = AtomicReference(HostConfiguration.getDefaultInstance()),
    private val loaded: AtomicBoolean = AtomicBoolean(false),
  ) {
    /** Provide a read-only view of the underlying bytes. */
    val byteview: ByteBuffer get() = bytes.asReadOnlyBuffer()

    /**
     *
     */
    fun load() {
      require(!loaded.get()) {
        "Cannot load native call bytes more than once"
      }
      loaded.compareAndSet(false, true)

      try {
        payload.set(HostConfiguration.parseFrom(byteview))
      } catch (ipbe: InvalidProtocolBufferException) {
        throw IllegalArgumentException("Failed to parse configuration payload", ipbe)
      } catch (e: Throwable) {
        throw IllegalStateException("Failed to load configuration payload", e)
      }
    }
  }

  // Apply this configuration to the provided builder.
  internal fun applyTo(target: HostConfiguration.Builder): HostConfiguration.Builder {
    return target
  }

  /** Companion methods for creating and parsing configurations from native data. */
  public companion object {
    /** */
    public const val DEFAULT_API_VERSION: String = "v1alpha1"

    /** */
    public val DEFAULT_PROTOCOL_MODE: ProtocolMode = ProtocolMode.PROTOBUF

    /**
     *
     */
    @JvmStatic public fun create(): NativeConfiguration = NativeConfiguration(DecodedNativeConfiguration(
      version = DEFAULT_API_VERSION,
      mode = DEFAULT_PROTOCOL_MODE,
    ))

    /**
     *
     */
    @JvmStatic public fun of(version: String, mode: ProtocolMode, byteview: ByteBuffer): NativeConfiguration =
      NativeConfiguration(DecodedNativeConfiguration(
        version = version,
        mode = mode,
        bytes = byteview,
      ).also {
        it.load()
      })
  }
}
