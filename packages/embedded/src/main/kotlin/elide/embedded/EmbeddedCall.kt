package elide.embedded

import elide.embedded.http.EmbeddedRequest
import elide.embedded.http.EmbeddedResponse

/** A unique incremental identifier for an [EmbeddedCall]. */
@JvmInline public value class EmbeddedCallId(public val value: Long)

/**
 * Represents an incoming call being dispatched through the embedded runtime. Calls provide a unique [id] and a
 * [request] to be processed, and produce an [EmbeddedResponse] when awaited.
 */
public interface EmbeddedCall {
  /** A unique identifier for this call. */
  public val id: EmbeddedCallId

  /** The [EmbeddedRequest] for this call, to be handled by a guest application. */
  public val request: EmbeddedRequest

  /**
   * Suspend until a response is available for this call. If a response has already been produced, this method returns
   * immediately without suspending.
   */
  public suspend fun await(): EmbeddedResponse
}

/**
 * A mutable [EmbeddedCall] which can be completed, providing a response that can be retrieved using [await].
 */
public interface CompletableEmbeddedCall : EmbeddedCall {
  /**
   * Complete this call with the provided [response]. This method is thread-safe and can be invoked from concurrent
   * coroutines without external synchronization.
   *
   * Only the first [complete] call will have effect, subsequent invocations will return `false` without changing the
   * call result.
   */
  public fun complete(response: EmbeddedResponse): Boolean
}