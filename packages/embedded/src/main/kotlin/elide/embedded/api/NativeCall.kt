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

import java.nio.ByteBuffer

/**
 *
 */
public class NativeCall private constructor (private val call: DecodedNativeCall) {
  @JvmRecord private data class DecodedNativeCall(
    val id: InFlightCallID,
  )

  public companion object {
    /**
     *
     */
    @JvmStatic public fun create(callId: Long): NativeCall = NativeCall(DecodedNativeCall(
      id = callId,
    ))

    /**
     *
     */
    @JvmStatic public fun of(callId: Long, byteview: ByteBuffer): NativeCall = NativeCall(DecodedNativeCall(
      id = callId,
    ))
  }
}
