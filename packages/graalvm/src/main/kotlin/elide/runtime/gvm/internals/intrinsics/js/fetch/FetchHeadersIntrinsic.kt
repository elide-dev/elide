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

package elide.runtime.gvm.internals.intrinsics.js.fetch

import org.graalvm.polyglot.Value
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMutableMultiMap
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchMutableHeaders
import elide.runtime.intrinsics.js.JsIterator
import elide.runtime.intrinsics.js.JsIterator.JsIteratorFactory
import elide.runtime.intrinsics.js.MultiMapLike
import elide.vm.annotations.Polyglot

/** Implementation of `Headers` intrinsic from the Fetch API. */
internal class FetchHeadersIntrinsic private constructor (
  initialData: JsMutableMultiMap<String, String>?,

  // Internal data map.
  private val data: JsMutableMultiMap<String, String> = initialData ?: allocateMap()
) : FetchMutableHeaders, MultiMapLike<String, String> by data {
  /** Factory for creating new mutable [FetchHeaders] implementations. */
  internal companion object Factory : FetchHeaders.Factory<FetchHeadersIntrinsic> {
    // Allocate an empty sorted map.
    @JvmStatic private fun allocateMap(): JsMutableMultiMap<String, String> = JsMutableMultiMap.empty()

    /**
     * Create an empty set of immutable fetch headers.
     *
     * @return Immutable and empty set of fetch headers.
     */
    @JvmStatic override fun empty(): FetchHeadersIntrinsic = FetchHeadersIntrinsic()

    /**
     * Create a set of immutable fetch headers from the provided set of [pairs].
     *
     * @return Immutable set of fetch headers.
     */
    @JvmStatic override fun fromPairs(pairs: Collection<Pair<String, String>>): FetchHeadersIntrinsic {
      return FetchHeadersIntrinsic(
        initialData = null,
        data = JsMutableMultiMap.fromPairs(pairs),
      )
    }

    /**
     * Create a set of immutable fetch headers from the provided set of [pairs].
     *
     * @return Immutable set of fetch headers.
     */
    @JvmStatic override fun from(vararg pairs: Pair<String, String>): FetchHeadersIntrinsic {
      return FetchHeadersIntrinsic(
        initialData = null,
        data = JsMutableMultiMap.fromPairs(pairs.toList()),
      )
    }

    /**
     * Create a set of immutable fetch headers from the provided [map] data.
     *
     * @return Immutable set of fetch headers.
     */
    @JvmStatic override fun fromMap(map: Map<String, String>): FetchHeadersIntrinsic {
      return FetchHeadersIntrinsic(
        initialData = null,
        data = JsMutableMultiMap.copyOf(map),
      )
    }

    /**
     * Create a set of immutable fetch headers from the provided [map] data.
     *
     * @return Immutable set of fetch headers.
     */
    @JvmStatic override fun fromMultiMap(map: Map<String, List<String>>): FetchHeadersIntrinsic {
      return FetchHeadersIntrinsic(
        initialData = null,
        data = JsMutableMultiMap.fromPairs(map.entries.flatMap {
          it.value.map { value ->
            it.key to value
          }
        }),
      )
    }

    /**
     * Create an immutable copy of the provided [previous] fetch headers.
     *
     * @return Immutable copy of the provided fetch headers.
     */
    @JvmStatic override fun from(previous: FetchHeaders): FetchHeadersIntrinsic {
      val concrete = previous as? FetchHeadersIntrinsic ?: error(
        "Failed to cast `FetchHeaders` as only known concrete class `FetchHeadersIntrinsic"
      )
      return FetchHeadersIntrinsic(
        initialData = null,
        data = concrete.internalDataForCopy(),
      )
    }
  }


  // Computed joined value cache.
  private val valueCache: SortedMap<String, String> = TreeMap(String.CASE_INSENSITIVE_ORDER)

  /** Empty constructor: empty headers. */
  @Polyglot constructor(): this(null)

  /** Construct from a plain JavaScript map-capable object. */
  @Polyglot constructor(initialValue: Value) : this (
    when {
      // if it's a host object, it should really only be another `FetchHeadersIntrinsic` instance.
      initialValue.isHostObject -> {
        (try {
          initialValue.`as`(FetchHeadersIntrinsic::class.java)
        } catch (err: ClassCastException) {
          throw IllegalArgumentException(
            "Unsupported type for `Headers` constructor: '${initialValue.metaObject.metaSimpleName}'"
          )
        }).internalDataForCopy()
      }

      // if it's a plain javascript object, we should be able to cast it to a map.
      initialValue.metaObject.metaQualifiedName == "Object" -> {
        val members = initialValue.memberKeys
        val buf = allocateMap()
        members.forEach {
          if (it.isNotEmpty()) {
            val value = initialValue.getMember(it)
            if (value != null && value.isString) {
              buf.append(it, value.asString())
            }
          }
        }
        buf
      }

      // if it's a regular map-like object, we can add each value wrapped in a single-entry set.
      initialValue.hasHashEntries() -> {
        val mapKeysIter = initialValue.hashKeysIterator
        val iterWrap = object: Iterator<String> {
          override fun hasNext(): Boolean = mapKeysIter.hasIteratorNextElement()
          override fun next(): String = mapKeysIter.iteratorNextElement.asString()
        }
        val buf = allocateMap()
        iterWrap.forEach { mapKey ->
          val mapValue = initialValue.getHashValueOrDefault(mapKey, null) ?: return@forEach
          if (mapValue.isString) {
            buf.append(mapKey, mapValue.asString())
          }
        }
        buf
      }

      // otherwise, we can't accept it, it's an error.
      else -> throw IllegalArgumentException(
        "Unsupported type for `Headers` constructor: '${initialValue.metaObject.metaSimpleName}'"
      )
    }
  )

  /** @return Copy of internal data. */
  private fun internalDataForCopy(): JsMutableMultiMap<String, String> = data

  // Whether the headers map is locked for iteration.
  private val locked: AtomicBoolean = AtomicBoolean(false)

  // Drop cached join values when they are touched.
  private fun trimCache(name: String) {
    if (valueCache.isNotEmpty()) {
      // value cache only has stuff in it if we have begun iterating at some point.
      valueCache.remove(name)
    }
  }

  // Lock the data structure when iteration begins.
  private fun <R: Any> lockForIteration(op: () -> R): R {
    return try {
      locked.compareAndSet(false, true)
      op.invoke()
    } finally {
      locked.compareAndSet(true, false)
    }
  }

  // Check the lock before mutating inner data.
  private fun <R: Any> onlyIfUnlocked(op: () -> R): R {
    check(!locked.get()) {
      "Cannot mutate headers while locked for iteration"
    }
    return op.invoke()
  }

  /** @inheritDoc */
  @Polyglot override fun keys(): JsIterator<String> = lockForIteration {
    JsIteratorFactory.forIterator(data.keys.iterator())
  }

  /** @inheritDoc */
  @Polyglot override fun values(): JsIterator<String> = lockForIteration {
    data.keys.map {
      get(it)!!  // safe to use !! here because we are iterating over known keys.
    }.let {
      JsIteratorFactory.forIterator(it.iterator())
    }
  }

  /** @inheritDoc */
  @Polyglot override fun append(name: String, value: String): Unit = onlyIfUnlocked {
    val has = data.has(name)
    data.append(name, value)
    if (has) trimCache(name)
  }

  /** @inheritDoc */
  @Polyglot override fun delete(name: String): Unit = onlyIfUnlocked {
    data.remove(name)
    trimCache(name)
  }

  /** @inheritDoc */
  @Polyglot override fun set(name: String, value: String): Unit = onlyIfUnlocked {
    val has = data.has(name)
    data.set(name, value)
    if (has) trimCache(name)
  }

  /** @inheritDoc */
  override fun render(flatten: Boolean): Map<String, List<String>> {
    return data.keys.stream().parallel().map {
      val value = data.getAll(it)
      it to if (flatten) {
        listOf(value.joinToString(", "))
      } else {
        value
      }
    }.collect(Collectors.toMap(
      { it.first },
      { it.second },
    ))
  }
}
