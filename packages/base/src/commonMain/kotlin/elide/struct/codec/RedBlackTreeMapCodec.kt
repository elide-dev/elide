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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import elide.struct.RedBlackTreeMap

internal class RedBlackTreeMapCodec<K : Comparable<K>, V>(
  keySerializer: KSerializer<K>,
  valueSerializer: KSerializer<V>,
) : KSerializer<RedBlackTreeMap<K, V>> {
  private val delegateSerializer = MapSerializer(keySerializer, valueSerializer)
  override val descriptor: SerialDescriptor = delegateSerializer.descriptor

  override fun deserialize(decoder: Decoder): RedBlackTreeMap<K, V> {
    return RedBlackTreeMap<K, V>().apply { putAll(delegateSerializer.deserialize(decoder)) }
  }

  override fun serialize(encoder: Encoder, value: RedBlackTreeMap<K, V>) {
    delegateSerializer.serialize(encoder, value)
  }
}
