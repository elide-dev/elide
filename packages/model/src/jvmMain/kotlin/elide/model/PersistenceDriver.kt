package elide.model

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.ListeningScheduledExecutorService
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.Descriptors
import com.google.protobuf.FieldMask
import com.google.protobuf.Message
import elide.model.cfg.FetchOptions
import elide.model.cfg.FetchOptions.MaskMode
import elide.model.cfg.DeleteOptions
import elide.model.cfg.UpdateOptions
import elide.model.cfg.WriteOptions
import elide.model.err.*
import elide.model.util.ModelMetadata.enforceRole
import elide.model.util.ModelMetadata.key
import elide.model.util.ModelMetadata.keyField
import elide.model.util.ModelMetadata.spliceIdBuilder
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.runtime.jvm.ReactiveFuture
import tools.elide.model.DatapointType
import tools.elide.model.FieldType
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.annotation.OverridingMethodsMustInvokeSuper

/**
 * Describes the surface of a generic persistence driver, which is capable of accepting arbitrary structured and typed
 * business data (also called "data models"), and managing them with regard to persistent storage, which includes
 * storing them when asked, and recalling them when subsequently asked to do so.
 *
 * Persistence driver implementations do not always guarantee *durability* of data. For example,
 * [CacheDriver] implementations are also [PersistenceDriver]s, and that entire class of implementations
 * does not guarantee data will be there when you ask for it *at all* (relying on cache state is generally
 * considered to be a very bad practice).
 *
 * Other implementation trees exist (notably, [DatabaseDriver]) which go the other way, and are expected to
 * guarantee durability of data across restarts, distributed systems and networks, and failure cases, as applicable.
 * Database driver implementations also support richer data storage features like querying and indexing.
 *
 * @see CacheDriver for persistence drivers with volatile durability guarantees
 * @see DatabaseDriver for drivers with rich features and/or strong durability guarantees.
 * @param Key Key record type (must be annotated with model role `OBJECT_KEY`).
 * @param Model Message/model type which this persistence driver is specialized for.
 **/
public interface PersistenceDriver<Key: Message, Model: Message> {
  /** Default model adapter internals.  */
  public object Internals {
    /** Log pipe for default model adapter.  */
    public val logging: Logger = Logging.of(PersistenceDriver::class)

    /**
     * Swallow any exceptions that occur
     *
     * @param operation Operation to run and wrap.
     */
    internal fun swallowExceptions(operation: DriverRunnable) {
      try {
        operation.run()
      } catch (exc: Exception) {
        val inner = if (exc.cause != null) exc.cause else exc
        logging.warn(
          String.format(
            "Encountered unidentified exception '%s'. Message: '%s'.",
            exc.javaClass.simpleName, exc.message
          )
        )
      }
    }

    /**
     * Convert async exceptions into persistence layer exceptions, according to the failure that occurred. Also print a
     * descriptive log statement.
     *
     * @param operation Operation to execute and wrap with protection.
     * @param R Return type for the callable operation, if applicable.
     * @return Return value of the async operation.
     **/
    @CanIgnoreReturnValue
    public fun <R> convertAsyncExceptions(operation: Callable<R>): R {
      return try {
        operation.call()
      } catch (ixe: InterruptedException) {
        logging.warn(
          String.format(
            "Interrupted. Message: '%s'.",
            ixe.message
          )
        )
        throw PersistenceOperationFailed.forErr(PersistenceFailure.INTERRUPTED)
      } catch (exe: ExecutionException) {
        val inner = if (exe.cause != null) exe.cause else exe
        logging.warn(
          String.format(
            "Encountered async exception '%s'. Message: '%s'.",
            inner!!.javaClass.simpleName, inner.message
          )
        )
        throw PersistenceOperationFailed.forErr(PersistenceFailure.INTERNAL)
      } catch (txe: TimeoutException) {
        throw PersistenceOperationFailed.forErr(PersistenceFailure.TIMEOUT)
      } catch (exc: Exception) {
        logging.warn(
          String.format(
            "Encountered unidentified exception '%s'. Message: '%s'.",
            exc.javaClass.simpleName, exc.message
          )
        )
        throw PersistenceOperationFailed.forErr(
          PersistenceFailure.INTERNAL,
          if (exc.cause != null) exc.cause else exc
        )
      }
    }

    /**
     * Enforce that a particular model operation have the provided value present, and equal to the expected value. If
     * these expectations are violated, an exception is thrown.
     *
     * @param value Value in the option set for this method.
     * @param expected Expected value from the option set.
     * @param expectation Message to throw if the expectation is violated.
     * @param R Return value type - same as `value` and `expected`.
     * @return Expected value if it is equal to `value`.
     **/
    @CanIgnoreReturnValue
    public fun <R> enforceOption(value: R?, expected: R, expectation: String): R {
      if (value != null && value == expected) {
        return value
      }
      throw IllegalArgumentException("Operation failed: $expectation")
    }

    /** Runnable that might throw async exceptions.  */
    internal fun interface DriverRunnable {
      /**
       * Run some operation that may throw async-style exceptions.
       *
       * @throws TimeoutException The operation timed out.
       * @throws InterruptedException The operation was interrupted during execution.
       * @throws ExecutionException An execution error halted async execution.
       */
      @Throws(TimeoutException::class, InterruptedException::class, ExecutionException::class)
      fun run()
    }
  }
  // -- API: Execution -- //

  /**
   * Resolve an executor service for use with this persistence driver. Operations will be executed against this as they
   * are received.
   *
   * @return Scheduled executor service.
   */
  public fun executorService(): ListeningScheduledExecutorService

  // -- API: Codec -- //

  /**
   * Acquire an instance of the codec used by this adapter. Codecs are either injected/otherwise provided during adapter
   * construction, or they are specified statically if the adapter depends on a specific codec.
   *
   * @return Model codec currently in use by this adapter.
   */
  public fun codec(): ModelCodec<*, *, *>

  // -- API: Key Generation -- //

  /**
   * Generate a semi-random opaque token, usable as an ID for a newly-created entity via the model layer. In this case,
   * the ID is returned directly, so it may be used to populate a key.
   *
   * @param instance Model instance to generate an ID for.
   * @return Generated opaque string ID.
   */
  public fun generateId(instance: Message?): String {
    return UUID.randomUUID().toString()
  }

  /**
   * Generate a key for a new entity, which must be stored by this driver, but does not yet have a key. If the driver
   * does not support key generation, [UnsupportedOperationException] is thrown.
   *
   * Generated keys are expected to be best-effort unique. Generally, Java's built-in [java.util.UUID] should
   * do the trick just fine. In more complex or scalable circumstances, this method can be overridden to reach out to
   * the data engine to generate a key.
   *
   * @param instance Default instance of the model type for which a key is desired.
   * @return Generated key for an entity to be stored.
   */
  public fun generateKey(instance: Message): Key {
    // enforce role, key field presence
    val descriptor = instance.descriptorForType
    enforceRole(descriptor, DatapointType.OBJECT)
    val keyType = keyField(descriptor)
    if (keyType.isEmpty)
      throw MissingAnnotatedField(descriptor, FieldType.KEY)

    // convert to builder, grab field builder for key (keys must be top-level fields)
    val builder = instance.newBuilderForType()
    val keyBuilder = builder.getFieldBuilder(keyType.get().field)
    spliceIdBuilder<Message.Builder, Any>(keyBuilder, Optional.of(generateId(instance)))
    @Suppress("UNCHECKED_CAST")
    val obj = keyBuilder.build() as Key
    Internals.logging.debug {
      "Generated key for record: '$obj'."
    }
    return obj
  }

  // -- API: Projections & Field Masking -- //

  /**
   * Apply the fields from `source` to `target`, considering any provided [FieldMask].
   *
   * If the invoking developer chooses to provide `markedPaths`, they must also supply `markEffect`. For
   * each field encountered that matches a property path in `markedPaths`, `markEffect` is applied. This
   * happens recursively for the entire model tree of `source` (and, consequently, `target`).
   *
   * After all field computations are complete, the builder is built (and casted, if necessary), before being handed
   * back to invoking code.
   *
   * @see FetchOptions.MaskMode Determines how "marked" fields are treated.
   *
   * @param target Builder to set each field value on, as appropriate.
   * @param source Source instance to pull fields and field values from.
   * @param markedPaths "Marked" paths - each one will be treated, as encountered, according to `markEffect`.
   * @param markEffect Determines how to treat "marked" paths. See [FetchOptions.MaskMode] for more information.
   * @param stackPrefix Dotted stack of properties describing the path that got us to this point (via recursion).
   * @return Constructed model, after applying the provided field mask, as applicable.
   */
  public fun applyFieldsRecursive(
    target: Message.Builder,
    source: Message,
    markedPaths: Set<String>,
    markEffect: MaskMode,
    stackPrefix: String
  ): Message.Builder {
    // otherwise, we must examine each field with a value on the `source`, checking against `markedPaths` (if present)
    // as we go. if it matches, we filter through `markEffect` before applying against `target`.
    for ((field, value) in source.allFields) {
      var effect: MaskMode =
        if (MaskMode.INCLUDE == markEffect) MaskMode.EXCLUDE else MaskMode.INCLUDE
      val currentPath = if (stackPrefix.isEmpty()) field.name else stackPrefix + "." + field.name
      val marked = markedPaths.contains(currentPath)
      if (Descriptors.FieldDescriptor.Type.MESSAGE != field.type && marked) {
        // field is in the marked paths.
        effect = markEffect
      } else if (Descriptors.FieldDescriptor.Type.MESSAGE == field.type) {
        effect = MaskMode.INCLUDE // always include messages
      }
      when (effect) {
        MaskMode.PROJECTION, MaskMode.INCLUDE -> {
          Internals.logging.debug {
            "Field '$currentPath' (${field.fullName}) included because it did not violate expectation " +
            "'${markEffect.name}' via field mask."
          }

          // handle recursive cases first
          if (Descriptors.FieldDescriptor.Type.MESSAGE == field.type) {
            target.setField(
              field,
              applyFieldsRecursive(
                target.getFieldBuilder(field),
                value as Message,
                markedPaths,
                markEffect,
                currentPath
              ).build()
            )
          } else {
            // it's a simple field value
            target.setField(field, value)
          }
        }
        MaskMode.EXCLUDE -> Internals.logging.debug {
          "Excluded field '$currentPath' (${field.fullName}) because it did not meet expectation " +
          "${markEffect.name} via field mask."
        }
      }
    }
    return target
  }

  /**
   * Apply mask-related options to the provided instance. This may include re-building *without* certain fields, so
   * the instance returned may be different.
   *
   * @param instance Instance to filter based on any provided field mask.k
   * @param options Options to apply to the provided instance.
   * @return Model, post-filtering.
   */
  @VisibleForTesting
  public fun applyMask(instance: Model, options: FetchOptions): Model {
    // do we have a mask to apply? does it have fields?
    if ((instance.isInitialized && options.fieldMask() != null) && options.fieldMask()!!.pathsCount > 0) {
      Internals.logging.trace {
        "Found valid field mask, applying: '${options.fieldMask()}'."
      }

      // resolve mask & mode
      val mask: FieldMask = options.fieldMask()!!
      val maskMode: MaskMode = Objects.requireNonNull(
        options.fieldMaskMode(),
        "Cannot provide `null` for field mask mode."
      )
      @Suppress("UNCHECKED_CAST")
      return applyFieldsRecursive(
        instance.newBuilderForType(),
        instance,
        ImmutableSet.copyOf(Objects.requireNonNull(mask.pathsList)),
        maskMode,
        "" /* root path */
      ).build() as Model
    }
    Internals.logging.trace {
      "No field mask found. Skipping mask application."
    }
    return instance
  }
  // -- API: Fetch -- //

  /**
   * Synchronously retrieve a data model instance from underlying storage, addressed by its unique ID.
   *
   * If the record cannot be located by the storage engine, `null` will be returned instead. For a safe variant
   * of this method (relying on [Optional]), see [.fetchSafe].
   *
   * **Note:** Asynchronous and reactive versions of this method also exist. You should always consider using
   * those if your requirements allow.
   *
   * @param key Key at which we should look for the requested entity, and return it if found.
   * @return Requested record, as a model instance, or `null` if one could not be found.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested instance.
   */
  @Throws(PersistenceException::class)
  public fun fetch(key: Key, options: FetchOptions = FetchOptions.DEFAULTS): Model? {
    return fetchSafe(key, options).orElse(null)
  }

  /**
   * Safely (and synchronously) retrieve a data model instance from storage, returning [Optional.empty] if it
   * cannot be located, rather than `null`.
   *
   * **Note:** Asynchronous and reactive versions of this method also exist. You should always consider using
   * those if your requirements allow. All of the reactive/async methods support null safety with [Optional].
   *
   * @see .fetch
   * @see .fetchAsync
   * @see .fetchReactive
   * @param key Key at which we should look for the requested entity, and return it if found.
   * @return Requested record, as a model instance, or [Optional.empty] if it cannot be found.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun fetchSafe(key: Key): Optional<Model> {
    return fetchSafe(key, FetchOptions.DEFAULTS)
  }

  /**
   * Safely (and synchronously) retrieve a data model instance from storage, returning [Optional.empty] if it
   * cannot be located, rather than `null`.
   *
   * This variant additionally allows specification of [FetchOptions].
   *
   * **Note:** Asynchronous and reactive versions of this method also exist. You should always consider using
   * those if your requirements allow. All of the reactive/async methods support null safety with [Optional].
   *
   * @see .fetch
   * @see .fetchAsync
   * @see .fetchReactive
   * @param key Key at which we should look for the requested entity, and return it if found.
   * @param options Options to apply to this individual retrieval operation.
   * @return Requested record, as a model instance, or [Optional.empty] if it cannot be found.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @Throws(PersistenceException::class)
  public fun fetchSafe(key: Key, options: FetchOptions = FetchOptions.DEFAULTS): Optional<Model> {
    Internals.logging.trace {
      "Synchronously fetching model with key '$key'. Options follow.\n${options}"
    }
    return Internals.convertAsyncExceptions {
      fetchAsync(key, options).get(
        options.timeoutValueMilliseconds() ?: DEFAULT_TIMEOUT,
        TimeUnit.MILLISECONDS
      )
    }
  }

  /**
   * Reactively retrieve a data model instance from storage, emitting it over a [Publisher] wrapped in an
   * [Optional].
   *
   * In other words, if the model cannot be located, exactly one [Optional.empty] will be emitted over the
   * channel. If the model is successfully located and retrieved, it is emitted exactly once. See other method variants,
   * which allow specification of additional options.
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Publisher] channel instead, to enable reactive exception handling.
   *
   * @param key Key at which we should look for the requested entity, and emit it if found.
   * @return Publisher which will receive exactly-one emitted [Optional.empty], or wrapped object.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun fetchReactive(key: Key): ReactiveFuture<Optional<Model>> {
    return fetchReactive(key, FetchOptions.DEFAULTS)
  }

  /**
   * Reactively retrieve a data model instance from storage, emitting it over a [Publisher] wrapped in an
   * [Optional].
   *
   * In other words, if the model cannot be located, exactly one [Optional.empty] will be emitted over the
   * channel. If the model is successfully located and retrieved, it is emitted exactly once. See other method variants,
   * which allow specification of additional options. This method variant additionally allows the specification of
   * [FetchOptions].
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Publisher] channel instead, to enable reactive exception handling.
   *
   * @see .fetch
   * @see .fetchAsync
   * @param key Key at which we should look for the requested entity, and emit it if found.
   * @param options Options to apply to this individual retrieval operation.
   * @return Publisher which will receive exactly-one emitted [Optional.empty], or wrapped object.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun fetchReactive(key: Key, options: FetchOptions): ReactiveFuture<Optional<Model>> {
    return this.fetchAsync(key, options)
  }

  /**
   * Asynchronously retrieve a data model instance from storage, which will populate the provided [Future] value.
   *
   * All futures emitted via the persistence framework (and Gust writ-large) are [ListenableFuture]-compliant
   * implementations under the hood. If the requested record cannot be located, [Optional.empty] is returned as
   * the future value, otherwise, the model is returned. See other method variants, which allow specification of
   * additional options.
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Future] channel instead, or raise the exception in the event
   * [Future.get] is called to surface it in the invoking (or dependent) code.
   *
   * @param key Key at which we should look for the requested entity, and emit it if found.
   * @return Future value, which resolves to the specified datamodel instance, or [Optional.empty] if the record
   * could not be located by the storage engine.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun fetchAsync(key: Key): ReactiveFuture<Optional<Model>> {
    return fetchAsync(key, FetchOptions.DEFAULTS)
  }

  /**
   * Asynchronously retrieve a data model instance from storage, which will populate the provided [Future] value.
   *
   * All futures emitted via the persistence framework (and Gust writ-large) are [ListenableFuture]-compliant
   * implementations under the hood. If the requested record cannot be located, [Optional.empty] is returned as
   * the future value, otherwise, the model is returned.
   *
   * This method additionally enables specification of custom [FetchOptions], which are applied on a per-
   * operation basis to override global defaults.
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Future] channel instead, or raise the exception in the event
   * [Future.get] is called to surface it in the invoking (or dependent) code.
   *
   * @see .fetch
   * @see .fetchSafe
   * @see .fetchReactive
   * @param key Key at which we should look for the requested entity, and emit it if found.
   * @param options Options to apply to this individual retrieval operation.
   * @return Future value, which resolves to the specified datamodel instance, or [Optional.empty] if the record
   * could not be located by the storage engine.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  @OverridingMethodsMustInvokeSuper
  public fun fetchAsync(key: Key, options: FetchOptions = FetchOptions.DEFAULTS): ReactiveFuture<Optional<Model>> {
    Internals.logging.trace {
      "Fetching model with key '$key' asynchronously. Options follow.\n${options}"
    }
    return retrieve(key, options)
  }

  /**
   * Low-level record retrieval method. Effectively called by all other fetch variants. Asynchronously retrieve a data
   * model instance from storage, which will populate the provided [ReactiveFuture] value.
   *
   * All futures emitted via the persistence framework (and Gust writ-large) are [ListenableFuture]-compliant
   * implementations under the hood. If the requested record cannot be located, [Optional.empty] is returned as
   * the future value, otherwise, the model is returned.
   *
   * This method additionally enables specification of custom [FetchOptions], which are applied on a per-
   * operation basis to override global defaults.
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Future] channel instead, or raise the exception in the event
   * [Future.get] is called to surface it in the invoking (or dependent) code.
   *
   * @param key Key at which we should look for the requested entity, and emit it if found.
   * @param options Options to apply to this individual retrieval operation.
   * @return Future value, which resolves to the specified datamodel instance, or [Optional.empty] if the record
   * could not be located by the storage engine.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun retrieve(key: Key, options: FetchOptions): ReactiveFuture<Optional<Model>>

  // -- API: Persist -- //

  /**
   * Create the record specified by `model` in underlying storage, provisioning a key or ID for the record if
   * needed. The persisted entity is returned or an error occurs.
   *
   * This operation will enforce the option `MUST_NOT_EXIST` for the write - i.e., "creating" a record implies
   * that it must not exist beforehand. Additionally, if the record is missing a unique ID or key (one or the other must
   * be annotated on the record), then a semi-random value will be generated for the record.
   *
   * The returned record will be re-constituted, with the spliced-in ID or key value, as applicable, and with any
   * computed or framework-related properties filled in (i.e. automatic timestamping).
   *
   * @param model Model to create in underlying storage. Requires a `ID` or `KEY`-annotated field.
   * @return Future value, which resolves to the stored model entity, affixed with an assigned ID or key.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while creating the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   */
  public fun create(model: Model): ReactiveFuture<Model> {
    return create(key<Key>(model).orElse(null), model)
  }

  /**
   * Create the record specified by `model` using the optional pre-fabricated `key`, in underlying storage.
   * If the provided key is empty or `null`, the engine will provision a key or ID for the record. The persisted
   * entity is returned or an error occurs.
   *
   * This operation will enforce the option `MUST_NOT_EXIST` for the write - i.e., "creating" a record implies
   * that it must not exist beforehand. Additionally, if the record is missing a unique ID or key (one or the other must
   * be annotated on the record), then a semi-random value will be generated for the record.
   *
   * The returned record will be re-constituted, with the spliced-in ID or key value, as applicable, and with any
   * computed or framework-related properties filled in (i.e. automatic timestamping).
   *
   * @param model Model to create in underlying storage. Requires a `ID` or `KEY`-annotated field.
   * @return Future value, which resolves to the stored model entity, affixed with an assigned ID or key.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while creating the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   */
  public fun create(key: Key?, model: Model): ReactiveFuture<Model> {
    return create(key, model, object : WriteOptions {
      override fun writeMode(): WriteOptions.WriteDisposition {
        return WriteOptions.WriteDisposition.MUST_NOT_EXIST
      }
    })
  }

  /**
   * Create the record specified by `model` using the specified set of `options`, in underlying storage. If
   * the provided mode's key or ID is empty or `null`, the engine will provision a key or ID for the record. The
   * persisted entity is returned or an error occurs.
   *
   * This operation will enforce the option `MUST_NOT_EXIST` for the write - i.e., "creating" a record implies
   * that it must not exist beforehand. Additionally, if the record is missing a unique ID or key (one or the other must
   * be annotated on the record), then a semi-random value will be generated for the record.
   *
   * The returned record will be re-constituted, with the spliced-in ID or key value, as applicable, and with any
   * computed or framework-related properties filled in (i.e. automatic timestamping).
   *
   * @param model Model to create in underlying storage. Requires a `ID` or `KEY`-annotated field.
   * @return Future value, which resolves to the stored model entity, affixed with an assigned ID or key.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while creating the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   */
  public fun create(model: Model, options: WriteOptions): ReactiveFuture<Model> {
    return create(key<Key>(model).orElse(null), model, options)
  }

  /**
   * Create the record specified by `model` using the optional pre-fabricated `key`, and making use of the
   * specified `options`, in underlying storage. If the provided key is empty or `null`, the engine will
   * provision a key or ID for the record. The persisted entity is returned or an error occurs.
   *
   * This operation will enforce the option `MUST_NOT_EXIST` for the write - i.e., "creating" a record implies
   * that it must not exist beforehand. Additionally, if the record is missing a unique ID or key (one or the other must
   * be annotated on the record), then a semi-random value will be generated for the record.
   *
   * The returned record will be re-constituted, with the spliced-in ID or key value, as applicable, and with any
   * computed or framework-related properties filled in (i.e. automatic timestamping).
   *
   * @param model Model to create in underlying storage. Requires a `ID` or `KEY`-annotated field.
   * @return Future value, which resolves to the stored model entity, affixed with an assigned ID or key.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while creating the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   * @throws IllegalArgumentException If an incompatible [WriteOptions.WriteDisposition] value is specified.
   */
  public fun create(key: Key?, model: Model, options: WriteOptions): ReactiveFuture<Model> {
    Internals.enforceOption(
      options.writeMode() ?: WriteOptions.WriteDisposition.MUST_NOT_EXIST,
      WriteOptions.WriteDisposition.MUST_NOT_EXIST,
      "Write options for `create` must specify `MUST_NOT_EXIST` write disposition."
    )
    return persist(key, model, options)
  }

  /**
   * Update the record specified by `model` in underlying storage, using the existing key or ID value affixed to
   * the model. The entity is returned in its updated form, or an error occurs.
   *
   * This operation will enforce the option `MUST_EXIST` for the write - i.e., "updating" a record implies that
   * it must exist beforehand. This means, if the record is missing a unique ID or key (one or the other must be
   * annotated on the record), then an error occurs (specifically, either [MissingAnnotatedField]) for a  missing
   * schema field, or [IllegalStateException] for a missing required value).
   *
   * The returned record will be re-constituted, with the ID or key value unmodified, as applicable, and with any
   * computed or framework-related properties updated in (i.e. automatic update timestamping).
   *
   * @param model Model to update in underlying storage. Requires a `ID` or `KEY`-annotated field and value.
   * @return Future value, which resolves to the stored model entity, after it has been updated.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while updated the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun update(model: Model): ReactiveFuture<Model> {
    return update(
      key<Key>(model).orElseThrow { IllegalStateException("Failed to resolve a key value for record.") } as Key,
      model)
  }

  /**
   * Update the record specified by `model` in underlying storage, making use of the specified `options`,
   * using the existing key or ID value affixed to the model. The entity is returned in its updated form, or an error
   * occurs.
   *
   * This operation will enforce the option `MUST_EXIST` for the write - i.e., "updating" a record implies that
   * it must exist beforehand. This means, if the record is missing a unique ID or key (one or the other must be
   * annotated on the record), then an error occurs (specifically, either [MissingAnnotatedField]) for a  missing
   * schema field, or [IllegalStateException] for a missing required value).
   *
   * The returned record will be re-constituted, with the ID or key value unmodified, as applicable, and with any
   * computed or framework-related properties updated in (i.e. automatic update timestamping).
   *
   * @param model Model to update in underlying storage. Requires a `ID` or `KEY`-annotated field and value.
   * @return Future value, which resolves to the stored model entity, after it has been updated.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while updated the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun update(model: Model, options: UpdateOptions): ReactiveFuture<Model> {
    return update(
      key<Key>(model).orElseThrow { IllegalStateException("Failed to resolve a key value for record.") } as Key,
      model,
      options
    )
  }

  /**
   * Update the record specified by `model`, and addressed by `key`, in underlying storage. The entity is
   * returned in its updated form, or an error occurs.
   *
   * This operation will enforce the option `MUST_EXIST` for the write - i.e., "updating" a record implies that
   * it must exist beforehand. This means, if the record is missing a unique ID or key (one or the other must be
   * annotated on the record), then an error occurs (specifically, either [MissingAnnotatedField]) for a  missing
   * schema field, or [IllegalStateException] for a missing required value).
   *
   * The returned record will be re-constituted, with the ID or key value unmodified, as applicable, and with any
   * computed or framework-related properties updated in (i.e. automatic update timestamping).
   *
   * @param model Model to update in underlying storage. Requires a `ID` or `KEY`-annotated field and value.
   * @return Future value, which resolves to the stored model entity, after it has been updated.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while updated the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun update(key: Key, model: Model): ReactiveFuture<Model> {
    return update(key, model, object : UpdateOptions {
      override fun writeMode(): WriteOptions.WriteDisposition {
        return WriteOptions.WriteDisposition.MUST_EXIST
      }
    })
  }

  /**
   * Update the record specified by `model`, and addressed by `key`, in underlying storage. The entity is
   * returned in its updated form, or an error occurs. This method variant additionally allows specification of custom
   * `options` for this individual operation.
   *
   * This operation will enforce the option `MUST_EXIST` for the write - i.e., "updating" a record implies that
   * it must exist beforehand. This means, if the record is missing a unique ID or key (one or the other must be
   * annotated on the record), then an error occurs (specifically, either [MissingAnnotatedField]) for a  missing
   * schema field, or [IllegalStateException] for a missing required value).
   *
   * The returned record will be re-constituted, with the ID or key value unmodified, as applicable, and with any
   * computed or framework-related properties updated in (i.e. automatic update timestamping).
   *
   * @param model Model to update in underlying storage. Requires a `ID` or `KEY`-annotated field and value.
   * @return Future value, which resolves to the stored model entity, after it has been updated.
   * @throws InvalidModelType If the specified model record is not usable with storage.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while updated the record.
   * @throws MissingAnnotatedField If a required annotated field cannot be located (i.e. `ID` or `KEY`).
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   * @throws IllegalArgumentException If an incompatible [WriteOptions.WriteDisposition] value is specified.
   */
  public fun update(key: Key, model: Model, options: UpdateOptions): ReactiveFuture<Model> {
    Internals.enforceOption(
      options.writeMode() ?: WriteOptions.WriteDisposition.MUST_EXIST,
      WriteOptions.WriteDisposition.MUST_EXIST,
      "Write options for `update` must specify `MUST_EXIST` write disposition."
    )
    return persist(key, model, options)
  }

  /**
   * Low-level record persistence method. Effectively called by all other create/put variants. Asynchronously write a
   * data model instance to storage, which will populate the provided [ReactiveFuture] value.
   *
   * Optionally, a key may be provided as a nominated value to the storage engine. Whether the engine accepts
   * nominated keys is up to the implementation. In all cases, the engine must return the key used to store and address
   * the value henceforth. If the engine *does* support nominated keys, it *must* operate in an idempotent
   * manner with regard to those keys. In other words, repeated calls to create the same entity with the same key will
   * not cause spurious side-effects - only one record will be created, with the remaining calls being rejected by the
   * underlying engine.
   *
   * All futures emitted via the persistence framework (and Gust writ-large) are [ListenableFuture]-compliant
   * implementations under the hood, but [ReactiveFuture] allows a model-layer result to be used as a
   * [Future], or a one-item reactive [Publisher].
   *
   * This method additionally enables specification of custom [WriteOptions], which are applied on a per-
   * operation basis to override global defaults.
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Future] channel instead, or raise the exception in the event
   * [Future.get] is called to surface it in the invoking (or dependent) code.
   *
   * @param key Key nominated by invoking code for storing this record. If no key is provided, the underlying storage
   * engine is expected to allocate one. Where unsupported, [PersistenceException] will be thrown.
   * @param model Model to store at the specified key, if provided.
   * @param options Options to apply to this persist operation.
   * @return Reactive future, which resolves to the key where the provided model is now stored. In no case should this
   * method return `null`. Instead, [PersistenceException] will be thrown.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while fetching the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   */
  public fun persist(key: Key?, model: Model, options: WriteOptions): ReactiveFuture<Model>

  // -- API: Delete -- //

  /**
   * Delete and fully erase the record referenced by `key` from underlying storage, permanently. The resulting
   * future resolves to the provided key value once the operation completes. If any issue occurs (besides encountering
   * an already-deleted entity, which is not an error), an exception is raised.
   *
   * @param key Key referring to the record which should be deleted, permanently, from underlying storage.
   * @return Future, which resolves to the provided key when the operation is complete.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while deleting the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun delete(key: Key): ReactiveFuture<Key> {
    return delete(key, DeleteOptions.DEFAULTS)
  }

  /**
   * Delete and fully erase the supplied `model` from underlying storage, permanently. The resulting future
   * resolves to the provided record's key value once the operation completes. If any issue occurs (besides encountering
   * an already-deleted entity, which is not an error), an exception is raised.
   *
   * @param model Model instance to delete from underlying storage.
   * @return Future, which resolves to the provided key when the operation is complete.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while deleting the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun deleteRecord(model: Model): ReactiveFuture<Key> {
    return deleteRecord(model, DeleteOptions.DEFAULTS)
  }

  /**
   * Delete and fully erase the supplied `model` from underlying storage, permanently. The resulting future
   * resolves to the provided record's key value once the operation completes. If any issue occurs (besides encountering
   * an already-deleted entity, which is not an error), an exception is raised.
   *
   * @param model Model instance to delete from underlying storage.
   * @param options Options to apply to this specific delete operation.
   * @return Future, which resolves to the provided key when the operation is complete.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while deleting the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun deleteRecord(model: Model, options: DeleteOptions): ReactiveFuture<Key> {
    return delete(
      key<Key>(model).orElseThrow { IllegalStateException("Cannot delete record with empty key/ID.") },
      options
    )
  }

  /**
   * Low-level record delete method. Effectively called by all other delete variants. Asynchronously and permanently
   * erase an existing data model instance from storage, addressed by its key unique key or ID.
   *
   * If no key or ID field, or value, may be located, an error is raised (see below for details). This operation is
   * expected to operate in an *idempotent* manner (i.e. repeated calls with identical parameters do not yield
   * different side effects). Calls referring to an already-deleted entity should silently succeed.
   *
   * All futures emitted via the persistence framework (and Gust writ-large) are [ListenableFuture]-compliant
   * implementations under the hood, but [ReactiveFuture] allows a model-layer result to be used as a
   * [Future], or a one-item reactive [Publisher].
   *
   * This method additionally enables specification of custom [DeleteOptions], which are applied on a per-
   * operation basis to override global defaults.
   *
   * **Exceptions:** Instead of throwing a [PersistenceException] as other methods do, this operation will
   * *emit* the exception over the [Future] channel instead, or raise the exception in the event
   * [Future.get] is called to surface it in the invoking (or dependent) code.
   *
   * @param key Unique key referring to the record in storage that should be deleted.
   * @param options Options to apply to this specific delete operation.
   * @return Future value, which resolves to the deleted record's key when the operation completes.
   * @throws InvalidModelType If the specified key type is not compatible with model-layer operations.
   * @throws PersistenceException If an unexpected failure occurs, of any kind, while deleting the requested resource.
   * @throws MissingAnnotatedField If the specified key record has no resolvable ID field.
   * @throws IllegalStateException If a required annotated field value cannot be resolved (i.e. an empty key or ID).
   */
  public fun delete(key: Key, options: DeleteOptions): ReactiveFuture<Key>

  public companion object {
    /** Default timeout to apply when otherwise unspecified.  */
    public const val DEFAULT_TIMEOUT: Long = 30

    /** Time units for [.DEFAULT_TIMEOUT].  */
    public val DEFAULT_TIMEOUT_UNIT: TimeUnit = TimeUnit.SECONDS

    /** Default timeout to apply when fetching from the cache.  */
    public const val DEFAULT_CACHE_TIMEOUT: Long = 5

    /** Time units for [.DEFAULT_CACHE_TIMEOUT].  */
    public val DEFAULT_CACHE_TIMEOUT_UNIT: TimeUnit = TimeUnit.SECONDS
  }
}
