/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded.api

import com.google.protobuf.InvalidProtocolBufferException
import tools.elide.app.EntrypointType
import tools.elide.call.v1alpha1.FetchRequest
import tools.elide.call.v1alpha1.FetchResponse
import tools.elide.call.v1alpha1.QueueInvocationRequest
import tools.elide.call.v1alpha1.QueueInvocationResponse
import tools.elide.call.v1alpha1.ScheduledInvocationRequest
import tools.elide.call.v1alpha1.ScheduledInvocationResponse
import tools.elide.call.v1alpha1.UnaryInvocation
import tools.elide.call.v1alpha1.UnaryInvocationRequest
import tools.elide.call.v1alpha1.UnaryInvocationRequestOrBuilder
import tools.elide.call.v1alpha1.UnaryInvocationResponse
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 *
 */
public class UnaryNativeCall private constructor (private val call: DecodedNativeCall) {
  /**
   *
   */
  public class UnaryNativeRequestBuilder(
    private val builder: UnaryInvocationRequest.Builder = UnaryInvocationRequest.newBuilder(),
  ): UnaryInvocationRequestOrBuilder by builder {
    /**
     *
     */
    public fun request(http: FetchRequest): UnaryNativeRequestBuilder = apply {
      builder.setFetch(http)
    }

    /**
     *
     */
    public fun request(alarm: ScheduledInvocationRequest): UnaryNativeRequestBuilder = apply {
      builder.setScheduled(alarm)
    }

    /**
     *
     */
    public fun request(queue: QueueInvocationRequest): UnaryNativeRequestBuilder = apply {
      builder.setQueue(queue)
    }

    // Build the final request structure.
    internal fun build(): UnaryInvocationRequest {
      return builder.build()
    }
  }

  /**
   *
   */
  @JvmRecord private data class DecodedNativeCall(
    private val id: InFlightCallID,
    private val mode: ProtocolMode,
    private val bytes: ByteBuffer = ByteBuffer.allocateDirect(0),
    private val payload: AtomicReference<UnaryInvocation> = AtomicReference(UnaryInvocation.getDefaultInstance()),
    private val responseData: AtomicReference<UnaryInvocationResponse.Builder> = AtomicReference(null),
    private val loaded: AtomicBoolean = AtomicBoolean(false),
    private val locked: AtomicBoolean = AtomicBoolean(false),
    private val freed: AtomicBoolean = AtomicBoolean(false),
  ) {
    /** Provide a read-only view of the underlying bytes. */
    val byteview: ByteBuffer get() = bytes.asReadOnlyBuffer()

    /**
     * Read the native call from the enclosed bytebuffer; if no call is present, fail.
     *
     * The enclosed call is expected to be an instance of
     */
    fun load() {
      require(!loaded.get()) {
        "Cannot load native call bytes more than once"
      }
      loaded.compareAndSet(false, true)

      try {
        UnaryInvocation.parseFrom(byteview).let {
          payload.set(it)
        }
      } catch (ipbe: InvalidProtocolBufferException) {
        throw IllegalArgumentException("Failed to parse call payload", ipbe)
      }
    }

    /** Whether this call is ready to be consumed. */
    val ready: Boolean get() = loaded.get() && !freed.get()

    /** Provides the native call ID, as tracked from the host application. */
    val callId: InFlightCallID get() = id

    /** Provides the protocol mode for the call. */
    val protocolMode: ProtocolMode get() = mode

    /** Provides the decoded invocation request for the call. */
    val invocation: UnaryInvocation get() = payload.get()

    /** Provides access to the request-side of the unary call. */
    val request: UnaryInvocationRequest get() = invocation.request

    /** Response structure builder. */
    val responseBuilder: UnaryInvocationResponse.Builder get() = responseData.get() ?: run {
      val builder = UnaryInvocationResponse.newBuilder()
      responseData.compareAndSet(null, builder)
      builder
    }

    /**
     *
     */
    fun <R> withActive(op: () -> R): R {
      return op.invoke()
    }

    /**
     *
     */
    fun <R> withLocked(op: () -> R): R {
      return op.invoke()
    }
  }

  /** Indicate the type of entrypoint this request is meant for. */
  public val entrypointType: EntrypointType? get() = when (call.request.requestCase) {
    UnaryInvocationRequest.RequestCase.FETCH -> EntrypointType.HTTP
    UnaryInvocationRequest.RequestCase.SCHEDULED -> EntrypointType.SCHEDULED
    UnaryInvocationRequest.RequestCase.QUEUE -> EntrypointType.QUEUE
    else -> null
  }

  /** Call ID accessor. */
  public val callId: InFlightCallID get() = call.callId

  /** Protocol mode accessor. */
  public val protocolMode: ProtocolMode get() = call.withActive { call.protocolMode }

  /** Request accessor. */
  public val request: UnaryInvocationRequest get() = call.withActive { call.request }

  /** Report call readiness. */
  public val ready: Boolean get() = call.ready

  /**
   *
   */
  public fun respond(response: FetchResponse): UnaryInvocationResponse.Builder = call.withLocked {
    require(entrypointType == EntrypointType.HTTP) {
      "Cannot respond with HTTP response for this request (expected: ${entrypointType?.name ?: "unknown"})"
    }
    call.responseBuilder.apply {
      setFetch(response)
    }
  }

  /**
   *
   */
  public fun respond(response: ScheduledInvocationResponse): UnaryInvocationResponse.Builder = call.withLocked {
    require(entrypointType == EntrypointType.SCHEDULED) {
      "Cannot respond with scheduled response for this request (expected: ${entrypointType?.name ?: "unknown"})"
    }
    call.responseBuilder.apply {
      setScheduled(response)
    }
  }

  /**
   *
   */
  public fun respond(response: QueueInvocationResponse): UnaryInvocationResponse.Builder = call.withLocked {
    require(entrypointType == EntrypointType.QUEUE) {
      "Cannot respond with queue response for this request (expected: ${entrypointType?.name ?: "unknown"})"
    }
    call.responseBuilder.apply {
      setQueue(response)
    }
  }

  /** Methods to decode or create native calls from scratch. */
  public companion object {
    /**
     *
     */
    @JvmStatic public fun create(callId: Long, mode: ProtocolMode): UnaryNativeCall = UnaryNativeCall(DecodedNativeCall(
      id = callId,
      mode = mode,
    ))

    /**
     *
     */
    @JvmStatic public fun buildRequest(
      callId: InFlightCallID,
      mode: ProtocolMode,
      builder: UnaryNativeRequestBuilder.() -> Unit = {},
    ): UnaryNativeCall {
      return UnaryNativeCall(DecodedNativeCall(
        id = callId,
        mode = mode,
        loaded = AtomicBoolean(true),
        payload = AtomicReference(UnaryInvocation.newBuilder().apply {
          request = UnaryNativeRequestBuilder().apply(builder).build()
        }.build()),
      ))
    }

    /**
     *
     */
    @JvmStatic public fun of(callId: Long, mode: ProtocolMode, byteview: ByteBuffer): UnaryNativeCall = UnaryNativeCall(
      DecodedNativeCall(
        id = callId,
        mode = mode,
        bytes = byteview,
      ).also {
        it.load()
      }
    )
  }
}
