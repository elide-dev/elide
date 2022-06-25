package elide.server.rpc.web

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.Futures
import com.google.protobuf.Message
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.rpc.RpcRuntime
import io.grpc.*
import io.grpc.protobuf.ProtoFileDescriptorSupplier
import io.grpc.protobuf.ProtoServiceDescriptorSupplier
import io.grpc.stub.AbstractStub
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.micronaut.context.annotation.Context
import jakarta.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.asDeferred
import java.io.ByteArrayInputStream
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of a [GrpcWebService] which calls the underlying gRPC service directly, in essence simulating an
 * incoming HTTP/2 gRPC request without engaging the TCP/IP stack.
 *
 * This connection manager leverages the [elide.server.rpc.RpcRuntime] to dispatch services over a stubbed "connection"
 * without leaving the JVM. The default service relay implementation works based on reflection, much like gRPC itself.
 * gRPC stubs are loaded by qualified class name (but still must be registered with [elide.server.rpc.RpcRuntime]).
 *
 * Once a class is loaded, a stub is acquired and connected to the channel provided by the [runtime]. If the class in
 * question cannot be loaded, or a stub cannot be acquired, [IllegalStateException] is thrown. See [fulfillAsync] for
 * more information.
 *
 * @see GrpcWebTransport for transport-oriented methods which are made available here.
 * @see fulfillAsync for details about how reflective fulfillment works.
 * @param runtime Active RPC runtime to use for service fulfillment.
 */
@Context internal class GrpcWebServiceRelay @Inject constructor(
  private val runtime: RpcRuntime
) : GrpcWebService {
  companion object {
    // Banned Java package paths for class loading.
    private val bannedPackages = sortedSetOf(
      "java",
      "javax",
      "org.graalvm",
      "sun",
    )
  }

  // Private logger.
  private val logging: Logger = Logging.of(GrpcWebServiceRelay::class)

  // Message deserializer.
  private val deserializer: MessageDeserializer = ReflectiveMessageDeserializer()

  // Return a set of error trailers, if any, for the provided `throwable`, otherwise, return `null`.
  @VisibleForTesting
  internal fun trailersFromThrowable(throwable: Throwable): Metadata? {
    return when (throwable) {
      is StatusRuntimeException -> throwable.trailers
      is StatusException -> throwable.trailers
      else -> null
    }
  }

  // Resolve a gRPC service binding to the Java package and class name that were generated for it.
  @VisibleForTesting
  internal fun resolveServiceJavaPackageAndName(serviceBinding: ServerServiceDefinition): Pair<String, String> {
    val descriptorSupplier = (serviceBinding.serviceDescriptor.schemaDescriptor as ProtoServiceDescriptorSupplier)
    val fileDescriptorSupplier = (serviceBinding.serviceDescriptor.schemaDescriptor as ProtoFileDescriptorSupplier)
    val fileDescriptor = fileDescriptorSupplier.fileDescriptor
    val serviceDescriptor = descriptorSupplier.serviceDescriptor
    val targetPackage = fileDescriptor.options.javaPackage.ifBlank {
      fileDescriptor.`package`
    }
    logging.debug {
      "Resolved package path '$targetPackage' for service '${serviceBinding.serviceDescriptor.name}'"
    }

    return (
      targetPackage to serviceDescriptor.name
    )
  }

  // Load the gRPC stub wrapper class from the classpath, and resolve the expected method from the class.
  @VisibleForTesting
  internal fun reflectivelyLoadGrpcClass(className: String): Class<*> {
    return try {
      val pathSplit = className.split(".")
      for (i in pathSplit.indices) {
        val path = pathSplit.slice(0 until i).joinToString(".")
        if (bannedPackages.contains(path)) {
          throw IllegalStateException(
            "Reflection-based gRPC service binding is not allowed for package or class '$path'"
          )
        }
      }

      logging.trace {
        "Loading class '$className' for gRPC stub"
      }
      Class.forName(className)
    } catch (err: Throwable) {
      logging.error(
        "Failed to load class for gRPC relay at name '$className'",
        err
      )
      throw Status.INTERNAL.withCause(
        err
      ).withDescription(
        "Service or method not found"
      ).asRuntimeException()
    }
  }

  // Load the appropriate method to dispatch (at `methodName`) from the provided `klass` definition.
  @VisibleForTesting
  internal fun reflectivelyLoadGrpcMethod(klass: Class<*>, methodName: String): Method {
    return try {
      klass.methods.find { it.name == (methodName.replaceFirstChar { char -> char.lowercase() }) }
        ?: throw IllegalStateException("Method not found: $methodName")
    } catch (err: Throwable) {
      logging.error(
        "Failed to resolve method at name '$methodName' from class '${klass.name}' for gRPC web dispatch",
        err
      )
      throw Status.INTERNAL.withCause(
        err
      ).withDescription(
        "Service or method not found"
      ).asRuntimeException()
    }
  }

  // Serialize a set of protocol buffer response messages as raw bytes.
  @VisibleForTesting
  internal fun serializeResponses(responses: List<Any>): ByteArray {
    val payload = if (responses.size > 1) {
      logging.warn("Multiple responses not supported at this time by the gRPC engine. Picking the first response.")
      responses.first()
    } else if (responses.size == 1) {
      responses.first()
    } else {
      return ByteArray(0)
    }

    return if (payload is Message) {
      payload.toByteArray()
    } else throw IllegalArgumentException(
      "No support for decoding non-message responses at this time. Instead, got instance of '${payload.javaClass.name}'"
    )
  }

  /**
   * Fulfill a single gRPC Web [call], using the provided [interceptor], and return the result to the caller; the
   * call object is not guaranteed to be the same.
   *
   * @param call gRPC web call which we should fulfill against the backing server.
   * @param interceptor Interceptor we should use to affix headers, etc.
   * @return Completed gRPC web call, either with an error or successful response payload.
   */
  @VisibleForTesting
  internal fun fulfillSingleCall(call: GrpcWebCall, interceptor: GrpcWebClientInterceptor): GrpcWebCall {
    // resolve the Java package path for the generated gRPC service we're invoking
    val (targetPackage, serviceName) = resolveServiceJavaPackageAndName(call.service)

    // prep metadata and attach a one-shot interceptor for it
    val metadata = MetadataUtil.metadataFromHeaders(call.httpRequest.headers)
    val extraInterceptors = if (metadata.keys().isNotEmpty()) {
      listOf(MetadataUtils.newAttachHeadersInterceptor(metadata))
    } else {
      emptyList()
    }

    // resolve the outer class definition, then create a stub from the embedded stub factory.
    val klass = reflectivelyLoadGrpcClass("$targetPackage.${serviceName}Grpc")
    val stubFactory = klass.getDeclaredMethod("newStub", Channel::class.java)
    val stub: AbstractStub<*> = runtime.prepareStub(
      stubFactory.invoke(null, call.channel) as AbstractStub<*>,
      listOf(interceptor).plus(extraInterceptors),
    )

    // pluck the desired method from the stub, prepare to de-serialize the request
    val grpcMethod = reflectivelyLoadGrpcMethod(
      stub.javaClass,
      call.method.methodDescriptor.fullMethodName.split("/").last(),
    )
    val stream = ByteArrayInputStream(call.httpRequest.body.orElseThrow())

    // begin by de-framing the request payload
    val deframer = MessageDeframer()
    val incomingMessage: Message = if (deframer.processInput(stream, call.contentType)) {
      deserializer.deserialize(
        grpcMethod,
        deframer.toByteArray()
      )
    } else {
      throw IllegalArgumentException(
        "Data stream for gRPC Web dispatch was malformed"
      )
    }

    // dispatch the method
    val observer = GrpcCallObserver(
      interceptor.latch,
    )

    // invoke the reflected method
    try {
      grpcMethod.invoke(
        stub,
        incomingMessage,
        observer,
      )
      if (!observer.await(call.config.timeout.seconds)) {
        throw Status.DEADLINE_EXCEEDED.withDescription(
          "CALL_TIMEOUT"
        ).asRuntimeException()
      }
    } catch (sre: StatusRuntimeException) {
      logging.debug {
        "Encountered error while executing gRPC method, of status '${sre.status.code.name}'"
      }
      throw sre
    }

    // if we get this far, the method executed, and now we can prepare a response by interrogating the observer to
    // find  out how the call went.
    val err = observer.error
    val responses = observer.values
    val terminalStatus = interceptor.terminalStatus.get()
    return if (observer.failed.get()) {
      logging.debug(
        "Encountered remote error from backing gRPC Web method '${call.method.methodDescriptor.fullMethodName}'",
        err
      )

      // try to synthesize the error into a gRPC status
      val errorHeaders = interceptor.headers
      val errorTrailers = interceptor.trailers

      call.notifyResponse(
        GrpcWebCallResponse.Error(
          call.contentType,
          terminalStatus,
          headers = errorHeaders,
          trailers = errorTrailers,
          cause = err.get(),
        )
      )
    } else {
      logging.debug {
        "Received total of ${responses.size} responses from backing gRPC Web method " +
          "'${call.method.methodDescriptor.fullMethodName}'. Relaying to client"
      }
      call.notifyResponse(
        GrpcWebCallResponse.UnaryResponse(
          call.contentType,
          payload = serializeResponses(responses),
          headers = interceptor.headers,
          trailers = interceptor.trailers,
        )
      )
    }
  }

  /** @inheritDoc */
  override fun channel(): ManagedChannel = runtime.inProcessChannel()

  /** @inheritDoc */
  override suspend fun fulfillAsync(call: GrpcWebCall, interceptor: GrpcWebClientInterceptor): Deferred<GrpcWebCall> {
    return Futures.immediateFuture(
      fulfillSingleCall(
        call,
        interceptor,
      )
    ).asDeferred()
  }

  /**
   * Observes a single gRPC Web dispatch cycle, buffering any response values provided via [onNext], and handling call
   * state via [onCompleted] and [onError].
   *
   * Once the call completes, the provided [latch] is notified, which lets dependent callers begin interrogating this
   * object to determine the outcome of the call.
   */
  private inner class GrpcCallObserver constructor (
    private val latch: CountDownLatch,
  ): StreamObserver<Any> {
    /** Set of values returned via [onNext]. */
    val values = LinkedList<Any>()

    /** Terminal error provided by [onError], if any. */
    val error: AtomicReference<Throwable> = AtomicReference(null)

    /** Error indicator. */
    val failed: AtomicBoolean = AtomicBoolean(false)

    /** Atomically flipped to `true` when the call closes. */
    private val completed: AtomicBoolean = AtomicBoolean(false)

    // Wait for the configured timeout duration for `latch` to signal method completion.
    fun await(timeout: Long): Boolean {
      return if (!latch.await(timeout, TimeUnit.SECONDS)) {
        logging.warn { "gRPC web call took more than $timeout seconds; assuming timeout" }
        false
      } else {
        true
      }
    }

    /** @inheritDoc */
    override fun onNext(value: Any) {
      if (!completed.get()) {
        values.add(value)
      }
    }

    /** @inheritDoc */
    override fun onError(t: Throwable) {
      completed.compareAndExchange(
        false,
        true,
      )
      failed.compareAndExchange(
        false,
        true,
      )
      error.set(t)
      latch.countDown()
    }

    /** @inheritDoc */
    override fun onCompleted() {
      completed.compareAndExchange(
        false,
        true,
      )
      latch.countDown()
    }
  }
}
