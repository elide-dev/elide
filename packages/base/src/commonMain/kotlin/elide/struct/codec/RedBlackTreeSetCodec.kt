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
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import elide.struct.RedBlackTreeSet

/** Serializer for the [RedBlackTreeSet] structure, using a [SetSerializer] delegate with a value serializer. */
internal class RedBlackTreeSetCodec<V : Comparable<V>>(
  valueSerializer: KSerializer<V>,
) : KSerializer<RedBlackTreeSet<V>> {
  private val delegateSerializer = SetSerializer(valueSerializer)
  override val descriptor: SerialDescriptor = delegateSerializer.descriptor

  override fun deserialize(decoder: Decoder): RedBlackTreeSet<V> {
    return RedBlackTreeSet<V>().apply { addAll(delegateSerializer.deserialize(decoder)) }
  }

  override fun serialize(encoder: Encoder, value: RedBlackTreeSet<V>) {
    delegateSerializer.serialize(encoder, value)
  }
}
