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
package elide.versions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import elide.runtime.core.HostPlatform

/** Serializer for [elide.runtime.core.HostPlatform]. */
internal class HostPlatformSerializer : KSerializer<HostPlatform> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("elide.runtime.core.HostPlatform", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: HostPlatform) = encoder.encodeString(value.platformString())

  override fun deserialize(decoder: Decoder): HostPlatform = HostPlatform.parsePlatform(decoder.decodeString())
}
