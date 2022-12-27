package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchMutableHeaders
import elide.runtime.intrinsics.js.JsIterator.JsIteratorFactory
import elide.runtime.intrinsics.js.JsIterator
import org.graalvm.polyglot.Value
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/** Implementation of `Headers` intrinsic from the Fetch API. */
internal class FetchHeadersIntrinsic private constructor (
  initialData: MutableMap<String, SortedSet<String>>?
) : FetchMutableHeaders {
  // Internal data map.
  private val data: MutableMap<String, SortedSet<String>> = initialData ?: allocateMap()

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
              buf[it] = sortedSetOf(value.asString())
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
            buf[mapKey] = sortedSetOf(mapValue.asString())
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
  private fun internalDataForCopy(): MutableMap<String, SortedSet<String>> {
    return TreeMap(data)
  }

  /** Typed constructors. */
  companion object {
    // Allocate an empty sorted map.
    @JvmStatic private fun allocateMap(): SortedMap<String, SortedSet<String>> = TreeMap(String.CASE_INSENSITIVE_ORDER)

    /** @return Empty `Headers` container. */
    @JvmStatic @Polyglot fun empty(): FetchHeadersIntrinsic = FetchHeadersIntrinsic()
  }

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
  @Polyglot override fun get(name: String): String? {
    val value = data[name]
    return if (value?.isNotEmpty() == true) {
      if (value.size == 1) {
        value.first()
      } else {
        valueCache.computeIfAbsent(name) {
          value.joinToString(", ")
        }
      }
    } else null
  }

  /** @inheritDoc */
  @Polyglot override fun has(name: String): Boolean = data.containsKey(name)

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
  @Polyglot override fun entries(): JsIterator<Array<String>> = lockForIteration {
    data.keys.map {
      arrayOf(it, get(it)!!)
    }.let {
      JsIteratorFactory.forIterator(it.iterator())
    }
  }

  /** @inheritDoc */
  @Polyglot override fun forEach(op: (value: String, name: String) -> Unit) {
    TODO("Functional header iteration is not implemented yet")
  }

  /** @inheritDoc */
  @Polyglot override fun forEach(op: (value: String, name: String, obj: FetchHeaders) -> Unit) {
    TODO("Functional header iteration is not implemented yet")
  }

  /** @inheritDoc */
  @Polyglot override fun append(name: String, value: String): Unit = onlyIfUnlocked {
    if (data.containsKey(name)) {
      data[name]!!.add(value)
      trimCache(name)  // only possibly present if there is already a value of some kind.
    } else {
      data[name] = sortedSetOf(value)
    }
  }

  /** @inheritDoc */
  @Polyglot override fun delete(name: String): Unit = onlyIfUnlocked {
    data.remove(name)
    trimCache(name)
  }

  /** @inheritDoc */
  @Polyglot override fun set(name: String, value: String): Unit = onlyIfUnlocked {
    if (data.containsKey(name)) {
      data[name]!!.add(value)
      trimCache(name)
    } else {
      data[name] = sortedSetOf(value)
    }
  }
}
