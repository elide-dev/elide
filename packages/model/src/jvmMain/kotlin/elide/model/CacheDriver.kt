@file:Suppress("UnstableApiUsage")

package elide.model

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.protobuf.Message
import elide.model.cfg.CacheOptions
import elide.model.cfg.FetchOptions
import elide.server.runtime.jvm.ReactiveFuture
import java.util.*

/**
 * Describes the surface of a *cache driver*, which is a partner object to a [PersistenceDriver] specifically tailored
 * to deal with caching engines.
 *
 * Cache drivers may be used with any [ModelAdapter] implementation for transparent read-through caching support.
 *
 * Caches implemented in this manner are expected to adhere to options defined on [CacheOptions], particularly with
 * regard to eviction and timeouts. Specific implementations may extend that interface to define custom options, which
 * may be provided to the implementation at runtime either via stubbed options parameters or app config.
 */
public interface CacheDriver<Key: Message, Model: Message> {
  /**
   * Flush the entire cache managed by this driver. This should drop all keys related to model instance caching that are
   * currently held by the cache.
   *
   * @param executor Executor to use for any async operations.
   * @return Future, which simply completes when the flush is done.
   */
  public fun flush(executor: ListeningScheduledExecutorService): ReactiveFuture<Unit>

  /**
   * Write a record (`model`) at `key` into the cache, overwriting any model currently stored at the same
   * key, if applicable. The resulting future completes with no value, when the cache write has finished, to let the
   * framework know the cache is done following up.
   *
   * @param key Key for the record we should inject into the cache.
   * @param model Record data to inject into the cache.
   * @param executor Executor to use for any async operations.
   * @return Future, which simply completes when the write is done.
   */
  public fun put(key: Key, model: Model, executor: ListeningScheduledExecutorService): ReactiveFuture<Unit>

  /**
   * Force-evict any cached record at the provided `key` in the cache managed by this driver. This operation is
   * expected to succeed in all cases and perform its work in an idempotent manner.
   *
   * @param key Key for the record to force-evict from the cache.
   * @param executor Executor to use for any async operations.
   * @return Future, which resolves to the evicted key when the operation completes.
   */
  public fun evict(key: Key, executor: ListeningScheduledExecutorService): ReactiveFuture<Key>

  /**
   * Force-evict the set of cached records specified by `keys`, in the cache managed by this driver. This
   * operation is expected to succeed in all cases and perform its work in an idempotent manner, similar to the single-
   * key version of this method (see: [.evict]).
   *
   * @param keys Set of keys to evict from the cache.
   * @param executor Executor to sue for any async operations.
   * @return Future, which simply completes when the bulk-evict operation is done.
   */
  public fun evict(keys: Iterable<Key>, executor: ListeningScheduledExecutorService): ReactiveFuture<List<Key>> {
    val evictions: MutableList<ReactiveFuture<Key>> = ArrayList<ReactiveFuture<Key>>()
    keys.forEach { key: Key -> evictions.add(evict(key, executor)) }
    return ReactiveFuture.wrap(Futures.allAsList(evictions))
  }

  /**
   * Attempt to resolve a known model, addressed by `key`, from the cache powered/backed by this driver, according
   * to `options` and making use of `executor`.
   *
   *
   * If no value is available in the cache, [Optional.empty] must be returned, which triggers a call to the
   * driver to resolve the record. If the record can be fetched originally, it will later be added to the cache by a
   * separate call to [.put].
   *
   * @param key Key for the record which we should look for in the cache.
   * @param options Options to apply to the fetch routine taking place.
   * @param executor Executor to use for async tasks. Provided by the driver or adapter.
   * @return Future value, which resolves either to [Optional.empty] or a wrapped result value.
   */
  public fun fetch(
    key: Key,
    options: FetchOptions?,
    executor: ListeningScheduledExecutorService?
  ): ReactiveFuture<Optional<Model>>
}
