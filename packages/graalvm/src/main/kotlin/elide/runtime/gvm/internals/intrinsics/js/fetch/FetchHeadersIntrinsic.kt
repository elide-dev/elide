package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchMutableHeaders
import elide.runtime.intrinsics.js.JsIterator.JsIteratorFactory
import elide.runtime.intrinsics.js.JsIterator
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/** Implementation of `Headers` intrinsic from the Fetch API. */
internal class FetchHeadersIntrinsic private constructor (
  private val data: MutableMap<String, SortedSet<String>>,
  private val valueCache: TreeMap<String, String> = TreeMap(),
) : FetchMutableHeaders {
  /** Typed constructors. */
  internal object HeadersConstructors {
    /** @return Empty `Headers` container. */
    @JvmStatic @Polyglot fun empty(): FetchHeadersIntrinsic = FetchHeadersIntrinsic(
      data = TreeMap(),
    )
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
