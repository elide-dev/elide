/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.struct.codec

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import elide.struct.PresortedList

/**
 *
 */
public class PresortedListCodec<V: Comparable<V>>(private val dataSerializer: KSerializer<V>)
  : KSerializer<PresortedList<V>> {
  // Delegate list serializer.
  private val delegateSerializer = ListSerializer(dataSerializer)

  @OptIn(ExperimentalSerializationApi::class)
  override val descriptor: SerialDescriptor get() = SerialDescriptor(
    "PresortedList",
    delegateSerializer.descriptor,
  )

  override fun deserialize(decoder: Decoder): PresortedList<V> {
    return PresortedList(delegateSerializer.deserialize(decoder))
  }

  override fun serialize(encoder: Encoder, value: PresortedList<V>) {
    delegateSerializer.serialize(encoder, value)
  }
}
