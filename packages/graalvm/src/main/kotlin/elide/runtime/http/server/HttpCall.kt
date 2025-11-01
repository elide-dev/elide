/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse

/**
 * An incoming call received by the server and dispatched to an [HttpCallHandler].
 *
 * Calls group a [request]/[response] pair and add a customizable [context] that can be used to store state bound to
 * its lifecycle. Handlers can consume the [requestBody] and set a source for the [responseBody] before [sending][send]
 * the response to the client.
 */
public interface HttpCall<C : CallContext> {
  /** Custom state attached by an [HttpApplication], bound to this specific call instance. */
  public val context: C

  /**
   * Request header received by the server. Can be used together with [requestBody] to access the full HTTP request
   * that initiated this call.
   */
  public val request: HttpRequest

  /**
   * Content for the incoming request that can be consumed asynchronously by an [ContentStreamConsumer].
   *
   * Once a consumer is attached to the body, it will collect data until the end of the stream is reached or the
   * consumer chooses to detach, discarding the rest of the data. After the consumer is released, any attempts to use
   * the request body will have no effect.
   */
  public val requestBody: ReadableContentStream // Request content

  /**
   * Response header to be sent to the client. Can be used together with [responseBody] to prepare the full HTTP
   * response for this call.
   *
   * Calling [sendHeaders] or [send] will flush this data to the client, after which modifying this object has no
   * effect.
   */
  public val response: HttpResponse // Netty response header

  /**
   * Content for the response sent to the client. Attach an [ContentStreamSource] as a source to provide the data.
   *
   * When [send] is called, the engine will pull from the source attached to the body to begin producing content; if
   * no source has been set, an empty body will be sent and the response will be closed immediately.
   *
   * After a producer is attached, releasing it will mark the end of the response body, which triggers the end of the
   * call once sent.
   */
  public val responseBody: WritableContentStream // Response content

  /**
   * Send the [response] headers to the client if they have not been sent yet. After this method is called any changes
   * to [response] will have no effect.
   *
   * Once the headers are sent by the underlying transport, the [context.headersSent()][CallContext.headersSent]
   * is called.
   *
   * Calling this method multiple times from concurrent threads is safe and will have no effect after the first
   * invocation.
   */
  public fun sendHeaders()

  /**
   * Begin sending the [responseBody] to the client, sending the headers first if they are not yet flushed. If the
   * [responseBody] has no attached producer, an empty body is sent instead.
   *
   * This method triggers the [context.contentFlushed()][CallContext.contentFlushed] event when the first body
   * chunk is handed to the transport, and the [context.contentSent()][CallContext.contentSent] event once the
   * final data chunk has finished sending.
   */
  public fun send()

  /**
   * Interrupt the call and mark it as failed, sending an HTTP 500 response if possible. A [cause] can be provided
   * for debugging purposes.
   */
  public fun fail(cause: Throwable? = null)
}

/**
 * Represents an application-specific context attached to an [HttpCall]. An [HttpApplication] creates a context
 * instance for each incoming call and can retrieve it via [HttpCall.context] to keep track of call-bound state.
 *
 * @see CallContext.Empty
 * @see HttpApplication
 * @see HttpCall
 */
public interface CallContext {
  /**
   * Called after the response headers are flushed to the transport. This event is always called before [headersSent]
   * and [contentFlushed].
   */
  public fun headersFlushed(): Unit = Unit

  /**
   * Called after the response headers are sent to the client. This event is always called before [contentFlushed].
   */
  public fun headersSent(): Unit = Unit

  /** Called right before the response content is sent. This event is always called before [contentSent]. */
  public fun contentFlushed(): Unit = Unit

  /**
   * Called when the entire response content has been sent to the client. This event is always called before
   * [callEnded].
   */
  public fun contentSent(): Unit = Unit

  /** Called when the call is closed to release any held resources. This is always the last event to be called. */
  public fun callEnded(failure: Throwable? = null): Unit = Unit

  /** Default empty context that can be used as a placeholder. */
  public data object Empty : CallContext
}
