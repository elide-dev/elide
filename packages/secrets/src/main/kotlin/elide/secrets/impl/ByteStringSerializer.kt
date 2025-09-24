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
package elide.secrets.impl

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class ByteStringSerializer : KSerializer<ByteString> {
  override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

  override fun serialize(encoder: Encoder, value: ByteString) {
    ByteArraySerializer().serialize(encoder, value.toByteArray())
  }

  override fun deserialize(decoder: Decoder): ByteString {
    return ByteString(ByteArraySerializer().deserialize(decoder))
  }
}
