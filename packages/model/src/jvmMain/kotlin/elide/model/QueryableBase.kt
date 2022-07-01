package elide.model

import com.google.protobuf.Message
import elide.model.cfg.QueryOptions
import elide.model.err.InvalidModelType
import elide.model.err.MissingAnnotatedField
import elide.model.err.PersistenceException
import elide.server.runtime.jvm.ReactiveFuture
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Defines the generic interface supplied for querying via both [DatabaseDriver] and [DatabaseAdapter] implementations;
 * not meant for end-consumption.
 */
public interface QueryableBase<Key: Message, Model: Message, Query> {
  /**
   * Execute the provided query asynchronously, producing a future which resolves to a lazy stream of keys-only
   * results, and applying the specified options.
   *
   *
   * Exceptions are returned as failed/rejected futures.
   *
   * @param query Query to execute and return key results for.
   * @param options Options to apply to the query execution.
   * @return Stream of results of type [Key].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun queryKeysAsync(query: Query, options: QueryOptions): ReactiveFuture<Stream<Key>>

  /**
   * Execute the provided query asynchronously, producing a future which resolves to a lazy stream of decoded record
   * results, and applying the specified options.
   *
   *
   * Exceptions are returned as failed/rejected futures.
   *
   * @param query Query to execute and return key results for.
   * @param options Options to apply to the query execution.
   * @return Stream of results of type [Model].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun queryAsync(query: Query, options: QueryOptions): ReactiveFuture<Stream<Model>>

  /**
   * Execute the provided query, producing a lazy stream of decoded and type-checked decoded record results, and
   * applying the specified options.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @param options Options to apply to the query execution.
   * @return Stream of results of type [Model].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun query(query: Query, options: QueryOptions): Stream<Model>

  /**
   * Execute the provided query, producing a lazy stream of decoded and type-checked keys-only results, and applying
   * the specified options.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @param options Options to apply to the query execution.
   * @return Stream of results of type [Key].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun queryKeys(query: Query, options: QueryOptions): Stream<Key>

  /**
   * Execute the provided query, producing a lazy stream of keys-only results; this method uses a default set of
   * sensible query options, which can be overridden via other method variants.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @return Stream of results of type [Key].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun queryKeys(query: Query): Stream<Key> {
    return this.queryKeys(query, QueryOptions.DEFAULTS)
  }

  /**
   * Synchronously execute the provided query, producing a lazy stream of keys-only results, and applying the
   * specified options, as applicable.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @param options Options to apply to the query execution.
   * @return Stream of results of type [Key].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun queryKeysSync(query: Query, options: QueryOptions): List<Key> {
    return this.queryKeys(query, options).collect(Collectors.toList())
  }

  /**
   * Synchronously execute the provided query, producing a lazy stream of keys-only results; this method uses a
   * default set of sensible query options, which can be overridden via other method variants.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @return Stream of results of type [Key].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun queryKeysSync(query: Query): List<Key> {
    return this.queryKeysSync(query, QueryOptions.DEFAULTS)
  }

  /**
   * Execute the provided query, producing a lazy stream of decoded record results; this method uses a default set of
   * sensible query options, which can be overridden via other method variants.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @return Stream of results of type [Model].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun query(query: Query): Stream<Model> {
    return this.query(query, QueryOptions.DEFAULTS)
  }

  /**
   * Synchronously execute the provided query, producing a lazy stream of decoded record results, and applying the
   * specified options, as applicable.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @param options Options to apply to the query execution.
   * @return Stream of results of type [Model].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun querySync(query: Query, options: QueryOptions): List<Model> {
    return this.query(query, options).collect(Collectors.toList())
  }

  /**
   * Synchronously execute the provided query, producing a lazy stream of decoded record results; this method uses a
   * default set of sensible query options, which can be overridden via other method variants.
   *
   *
   * Because this method is synchronous, exceptions are thrown. For safer or more performant options, take a look
   * at the async and safe versions of this method.
   *
   * @param query Query to execute and return key results for.
   * @return Stream of results of type [Model].
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun querySync(query: Query): List<Model> {
    return this.querySync(query, QueryOptions.DEFAULTS)
  }
}
