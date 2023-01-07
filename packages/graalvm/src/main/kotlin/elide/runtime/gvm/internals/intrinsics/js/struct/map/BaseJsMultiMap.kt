package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.JsIterator
import elide.runtime.intrinsics.js.MapLike
import kotlinx.collections.immutable.toImmutableList
import java.util.stream.Stream

/** Abstract implementation of a JS-compatible multi-map structure. */
internal abstract class BaseJsMultiMap<K: Any, V>(
  @Volatile protected var backingMap: Map<K, List<V>>,
  sorted: Boolean,
  mutable: Boolean,
  threadsafe: Boolean,
) : AbstractJsMultiMap<K, V>(sorted, mutable, threadsafe) {
  /** @inheritDoc */
  override fun keysStream(parallel: Boolean): Stream<K> = BaseJsMap.toStream(
    backingMap.keys.stream(),
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun keysSequence(): Sequence<K> = backingMap.keys.asSequence()

  /** @inheritDoc */
  override fun valuesStream(parallel: Boolean): Stream<V>  = BaseJsMap.toStream(
    backingMap.values.stream().flatMap {
      if (parallel) it.parallelStream()
      else it.stream()
    },
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun valuesSequence(): Sequence<V> = backingMap.values.flatten().asSequence()

  /** @inheritDoc */
  override fun containsKey(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  override fun containsValue(value: V): Boolean = backingMap.values.any { it.contains(value) }

  /** @inheritDoc */
  override fun isEmpty(): Boolean = backingMap.isEmpty()

  /** @inheritDoc */
  override val entries: Set<Map.Entry<K, V>> get() = backingMap.entries.flatMap {
    it.value.map { valueEntry ->
      object: Map.Entry<K, V> {
        override val key: K = it.key
        override val value: V = valueEntry
      }
    }
  }.toSet()

  /** @inheritDoc */
  override val keys: Set<K> get() = backingMap.keys

  /** @inheritDoc */
  override val values: Collection<V> get() = backingMap.values.flatten()

  /** @inheritDoc */
  @get:Polyglot override val size: Int get() = backingMap.size

  /** @inheritDoc */
  override fun get(key: K): V? = backingMap[key]?.firstOrNull()

  /** @inheritDoc */
  override fun getOrDefault(key: K, defaultValue: V): V = backingMap[key]?.firstOrNull() ?: defaultValue

  /** @inheritDoc */
  @Polyglot override fun getAll(key: K): List<V> = backingMap[key]?.toImmutableList() ?: emptyList()

  /** @inheritDoc */
  override fun has(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  override fun keys(): JsIterator<K> = JsIterator.JsIteratorFactory.forIterator(
    backingMap.keys.iterator()
  )

  /** @inheritDoc */
  override fun values(): JsIterator<V> = JsIterator.JsIteratorFactory.forIterator(
    backingMap.values.flatten().iterator()
  )

  /** @inheritDoc */
  override fun entries(): JsIterator<MapLike.Entry<K, V>> = JsIterator.JsIteratorFactory.forIterator(
    backingMap.entries.flatMap {
      it.value.map { valueEntry ->
        object: MapLike.Entry<K, V> {
          override val key: K = it.key
          override val value: V = valueEntry
        }
      }
    }.iterator()
  )

  /** @inheritDoc */
  override fun forEach(op: (MapLike.Entry<K, V>) -> Unit) = entries.forEach {
    op.invoke(BaseJsMap.entry(
      it.key,
      it.value,
    ))
  }

  /**
   * TBD.
   */
  @Polyglot abstract override fun toString(): String
}
