@file:Suppress("UnstableApiUsage")

package elide.model

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.protobuf.Message
import elide.model.cfg.DeleteOptions
import elide.model.cfg.FetchOptions
import elide.model.cfg.WriteOptions
import elide.model.util.ModelMetadata.enforceRole
import elide.model.util.ModelMetadata.id
import elide.server.runtime.jvm.ReactiveFuture
import tools.elide.model.DatapointType
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Specifies an adapter for data models. "Adapters" are responsible for handling data storage and recall, and generic
 * model serialization and deserialization activities.
 *
 * Adapters are composed of a handful of components, which together define the functionality that composes the adapter
 * writ-large.
 *
 * <p>Major components of functionality are described below:
 * <ul>
 *   <li><b>Codec:</b> The [ModelCodec] is responsible for serialization and deserialization. In some cases, codecs can
 *   be mixed with other objects to customize how data is stored. For example, the Redis cache layer supports using
 *   ProtoJSON, Protobuf binary, or JVM serialization, optionally with compression. On the other hand, the Firestore
 *   adapter specifies its own codecs which serialize into Firestore models.</li>
 *   <li><b>Driver:</b> The [PersistenceDriver] is responsible for persisting serialized/collapsed models into
 *   underlying storage, deleting data recalling data via key fetches, and querying indexes to produce result-sets.</li>
 * </ul></p>
 *
 * @see PersistenceDriver Interface which defines basic driver functionality.
 * @see CacheDriver Cache-specific persistence driver support, included in this object.
 * @see DatabaseAdapter Extends this interface with richer data engine features.
 * @param Key Key type, instances of which uniquely address instances of {@code Model}.
 * @param Model Model type which this adapter is responsible for adapting.
 */
public interface ModelAdapter<Key: Message, Model: Message>: PersistenceDriver<Key, Model> {
  // -- Interface: Drivers -- //
  /**
   * Return the cache driver in use for this particular model adapter. If a cache driver is present, and active/enabled
   * according to database driver settings, it will be used on read-paths (such as fetching objects by ID).
   *
   * @return Cache driver currently in use by this model adapter.
   */
  public fun cache(): Optional<CacheDriver<Key, Model>>

  /**
   * Return the lower-level [PersistenceDriver] powering this adapter. The driver is responsible for communicating
   * with the actual backing storage service, either via local stubs/emulators or a production API.
   *
   * @return Persistence driver instance currently in use by this model adapter.
   */
  public fun engine(): PersistenceDriver<Key, Model>

  // -- Interface: Execution -- //

  /** @inheritDoc  */
  override fun executorService(): ListeningScheduledExecutorService {
    return engine().executorService()
  }

  // -- Interface: Key Generation -- //

  /** @inheritDoc  */
  override fun generateKey(instance: Message): Key {
    return engine().generateKey(instance)
  }

  // -- Interface: Fetch -- //

  /** @inheritDoc  */
  override fun retrieve(key: Key, options: FetchOptions): ReactiveFuture<Optional<Model>> {
    enforceRole(key, DatapointType.OBJECT_KEY)
    val exec: ListeningScheduledExecutorService = executorService()
    PersistenceDriver.Internals.logging.trace {
      "Retrieving record '${id<java.io.Serializable>(key)}' from storage (executor: '$exec')..."
    }
    val cache = cache()
    return if (options.enableCache() && cache.isPresent) {
      PersistenceDriver.Internals.logging.debug {
        "Caching enabled with object of type '${cache.get().javaClass.simpleName}'."
      }

      // cache result future
      val cacheFetchFuture = Objects.requireNonNull(
        cache.get().fetch(key, options, exec), "Cache cannot return `null` for `retrieve`."
      )

      // wrap in a future, with a non-propagating cancelling timeout, which handles any nulls from the cache.
      val cacheFuture: ListenableFuture<Optional<Model>> = Futures.nonCancellationPropagating(
        Futures.transform(cacheFetchFuture, {
          if (it != null && it.isPresent) {
            it
          }
          else
            // not found
            Optional.empty<Model>()
        }, exec)
      )

      // wrap the cache future in a timeout function, which enforces the configured (or default) cache timeout
      val limitedCacheFuture = Futures.withTimeout(
        cacheFuture,
        options.cacheTimeoutMilliseconds() ?: PersistenceDriver.DEFAULT_CACHE_TIMEOUT,
        TimeUnit.MILLISECONDS,
        exec
      )

      // finally, respond to a cache miss by deferring to the driver directly. this must be separate from `cacheFuture`
      // to allow separate cancellation of the cache future and the future which backstops it.
      ReactiveFuture.wrap(
        Futures.transformAsync(limitedCacheFuture,
          { cacheResult ->
            PersistenceDriver.Internals.logging.debug {
              "Returning response from cache (value present: '${cacheResult?.isPresent ?: "null"}')"
            }
            if (cacheResult != null && cacheResult.isPresent) {
              Futures.immediateFuture<Optional<Model>>(cacheResult)
            } else {
              val record = engine().retrieve(key, options)
              record.addListener({
                PersistenceDriver.Internals.logging.debug(
                  "Response was NOT cached. Storing in cache..."
                )
                PersistenceDriver.Internals.swallowExceptions {
                  val fetchResult = record.get()
                  fetchResult.ifPresent { model: Model ->
                    cache.get().put(
                      key,
                      model,
                      executorService()
                    )
                  }
                }
              }, executorService())
              record
            }
          }, exec
        ), exec
      )
    } else {
      PersistenceDriver.Internals.logging.debug("Caching is disabled. Deferring to driver.")
      engine().retrieve(key, options)
    }
  }

  // -- Interface: Persist -- //

  /** @inheritDoc  */
  override fun persist(key: Key?, model: Model, options: WriteOptions): ReactiveFuture<Model> {
    return engine().persist(key, model, options)
  }

  /** @inheritDoc  */
  override fun delete(key: Key, options: DeleteOptions): ReactiveFuture<Key> {
    val op = engine().delete(key, options)
    if (options.enableCache()) {
      // if caching is enabled and a cache driver is present, make sure to evict any cached record behind this key.
      val cacheDriver = cache()
      if (cacheDriver.isPresent) {
        val exec: ListeningScheduledExecutorService = executorService()
        val storageDelete = engine().delete(key, options)
        val cacheEvict = cacheDriver.get().evict(key, exec)
        return ReactiveFuture.wrap(
          Futures.whenAllComplete(
            storageDelete,
            cacheEvict
          ).call({ key }, exec)
        )
      }
    }
    return op
  }
}
