package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.JsIterator
import elide.runtime.intrinsics.js.MapLike
import kotlinx.collections.immutable.toImmutableList
import java.util.stream.Stream

/**
 * TBD.
 */
internal abstract class BaseJsMultiMap<K: Any, V>(
  map: Map<K, List<V>>,
  sorted: Boolean,
  mutable: Boolean,
  threadsafe: Boolean,
) : AbstractJsMultiMap<K, V>(sorted, mutable, threadsafe) {
  // The backing map.
  @Volatile protected var backingMap: Map<K, List<V>> = map

  /** @inheritDoc */
  override fun resolve(key: K): V? = backingMap[key]?.firstOrNull()

  /** @inheritDoc */
  override fun onResolve(key: K, value: V?) = Unit  // no-op by default

  /** @inheritDoc */
  override fun keysStream(parallel: Boolean): Stream<K> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun keysSequence(): Sequence<K> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun valuesStream(parallel: Boolean): Stream<V> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun valuesSequence(): Sequence<V> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun containsKey(key: K): Boolean {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun containsValue(value: V): Boolean {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun isEmpty(): Boolean {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override val entries: Set<Map.Entry<K, V>>
    get() = TODO("Not yet implemented")

  /** @inheritDoc */
  override val keys: Set<K>
    get() = TODO("Not yet implemented")

  /** @inheritDoc */
  override val values: Collection<V>
    get() = TODO("Not yet implemented")

  /** @inheritDoc */
  override val size: Int
    get() = TODO("Not yet implemented")

  /** @inheritDoc */
  override fun get(key: K): V? {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  @Polyglot override fun getAll(key: K): List<V> = backingMap[key]?.toImmutableList() ?: emptyList()

  /** @inheritDoc */
  override fun has(key: K): Boolean {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun keys(): JsIterator<K> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun values(): JsIterator<V> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun entries(): JsIterator<MapLike.Entry<K, V>> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun forEach(op: (MapLike.Entry<K, V>) -> Unit) {
    TODO("Not yet implemented")
  }

  /**
   * TBD.
   */
  @Polyglot abstract override fun toString(): String
}
