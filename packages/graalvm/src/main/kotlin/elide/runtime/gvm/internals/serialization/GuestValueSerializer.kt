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
package elide.runtime.gvm.internals.serialization

import org.graalvm.polyglot.Value
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.encoding.encodeStructure

public data object GuestValueSerializer : SerializationStrategy<Value> {
  @OptIn(InternalSerializationApi::class)
  override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Value")

  @OptIn(ExperimentalSerializationApi::class)
  override fun serialize(encoder: Encoder, value: Value): Unit = when {
    value.isNull -> encoder.encodeNull()
    value.isString -> encoder.encodeString(value.asString())
    value.isBoolean -> encoder.encodeBoolean(value.asBoolean())
    value.isNumber -> when {
      value.fitsInByte() -> encoder.encodeByte(value.asByte())
      value.fitsInShort() -> encoder.encodeShort(value.asShort())
      value.fitsInInt() -> encoder.encodeInt(value.asInt())
      value.fitsInLong() -> encoder.encodeLong(value.asLong())
      value.fitsInFloat() -> encoder.encodeFloat(value.asFloat())
      value.fitsInDouble() -> encoder.encodeDouble(value.asDouble())
      else -> throw SerializationException("Cannot serialize number that does not fit in a primitive: $value")
    }

    value.hasArrayElements() -> encoder.encodeCollection(
      descriptor = listSerialDescriptor(descriptor),
      collectionSize = value.arraySize.toInt(),
    ) {
      for (i in 0 until value.arraySize)
        encodeSerializableElement(descriptor, i.toInt(), this@GuestValueSerializer, value.getArrayElement(i))
    }

    value.hasHashEntries() -> {
      fun keyToString(value: Value) = if (value.isString) value.asString() else value.toString()

      val descriptor = buildClassSerialDescriptor("Value") {
        val keys = value.hashKeysIterator
        while (keys.hasIteratorNextElement()) element(keyToString(keys.iteratorNextElement), descriptor)
      }

      encoder.encodeStructure(descriptor) {
        val keys = value.hashKeysIterator
        while (keys.hasIteratorNextElement()) {
          val key = keys.iteratorNextElement
          encodeNullableSerializableElement(
            descriptor = descriptor,
            index = descriptor.getElementIndex(keyToString(key)),
            serializer = this@GuestValueSerializer,
            value = value.getHashValue(key),
          )
        }
      }
    }

    value.hasMembers() -> {
      val descriptor = buildClassSerialDescriptor("Value") {
        value.memberKeys.forEach { key -> element(key, descriptor) }
      }

      encoder.encodeStructure(descriptor) {
        value.memberKeys.forEach { key ->
          encodeNullableSerializableElement(
            descriptor = descriptor,
            index = descriptor.getElementIndex(key),
            serializer = this@GuestValueSerializer,
            value = value.getMember(key),
          )
        }
      }
    }

    else -> throw SerializationException("Cannot serialize non-guest values")
  }
}
