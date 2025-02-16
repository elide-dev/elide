/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js.stream

import org.graalvm.polyglot.Value
import java.util.concurrent.Future
import elide.runtime.exec.GuestExecutor
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WebStreamsIntrinsic
import elide.runtime.intrinsics.js.ReadableStream
import elide.vm.annotations.Polyglot

/**
 * ## Readable Stream Source
 *
 * This interface implements API-exposed methods for a custom [ReadableStream]; such streams consult an object of this
 * type in order to produce their data.
 *
 * For more information, see the `underlyingSource` object within the [ReadableStream] constructor documentation (linked
 * below).
 *
 * From MDN:
 * An object containing methods and properties that define how the constructed stream instance will behave.
 * `underlyingSource` can contain the following:
 *
 * - `[start]` (controller)
 *   This is a method, called immediately when the object is constructed. The contents of this method are defined by the
 *   developer, and should aim to get access to the stream source, and do anything else required to set up the stream
 *   functionality. If this process is to be done asynchronously, it can return a promise to signal success or failure.
 *   The controller parameter passed to this method is a [ReadableStreamDefaultController] or a
 *   [ReadableByteStreamController], depending on the value of the type property. This can be used by the developer to
 *   control the stream during set up.
 *
 * - `[pull]` (controller)
 *   This method, also defined by the developer, will be called repeatedly when the stream's internal queue of chunks is
 *   not full, up until it reaches its high water-mark. If pull() returns a promise, then it won't be called again until
 *   that promise fulfills; if the promise rejects, the stream will become errored. The controller parameter passed to
 *   this method is a [ReadableStreamDefaultController] or a [ReadableByteStreamController], depending on the value of
 *   the [type] property. This can be used by the developer to control the stream as more chunks are fetched. This
 *   function will not be called until [start] successfully completes. Additionally, it will only be called repeatedly
 *   if it enqueues at least one chunk or fulfills a BYOB request; a no-op [pull] implementation will not be continually
 *   called.
 *
 * - `[cancel]` (reason)
 *   This method, also defined by the developer, will be called if the app signals that the stream is to be cancelled
 *   (e.g. if [ReadableStream.cancel] is called). The contents should do whatever is necessary to release access to the
 *   stream source. If this process is asynchronous, it can return a promise to signal success or failure. The reason
 *   parameter contains a string describing why the stream was cancelled.
 *
 * - `[type]` (string)
 *   This property controls what type of readable stream is being dealt with. If it is included with a value set to
 *   `"bytes"`, the passed controller object will be a [ReadableByteStreamController] capable of handling a BYOB (bring
 *   your own buffer)/byte stream. If it is not included, the passed controller will be a
 *   [ReadableStreamDefaultController].
 *
 * - `[autoAllocateChunkSize]` (optional unsigned long)
 *   For byte streams, the developer can set the [autoAllocateChunkSize] with a positive integer value to turn on the
 *   stream's auto-allocation feature. With this is set, the stream implementation will automatically allocate a view
 *   buffer of the specified size in [ReadableByteStreamController.byobRequest] when required. This must be set to
 *   enable zero-copy transfers to be used with a default [ReadableStreamDefaultReader]. If not set, a default reader
 *   will still stream data, but [ReadableByteStreamController.byobRequest] will always be `null` and transfers to the
 *   consumer must be via the stream's internal queues.
 */
public interface ReadableStreamSource {
  /**
   * Type of stream source being dealt with; one of [StreamSourceType.BYTES] or [StreamSourceType.DEFAULT].
   */
  @get:Polyglot public val type: StreamSourceType

  /**
   * Starts the stream source; can return a promise if initialization is asynchronous.
   *
   * @return Any value, or a promise which resolves to any value
   */
  @Polyglot public fun start(controller: ReadableStreamController): Future<Any?>?

  /**
   * Pulls from the stream source; can return a promise if pulling is asynchronous.
   *
   * @return Any value, or a promise which resolves to any value
   */
  @Polyglot public fun pull(controller: ReadableStreamController): Future<Any?>?

  /**
   * Cancels the stream source; can return a promise if cancellation is asynchronous.
   *
   * @param reason String describing the reason for cancellation
   * @return Any value, or a promise which resolves to any value
   */
  @Polyglot public fun cancel(reason: String?): Future<Any?>?

  /** Utility methods for creating [ReadableStreamSource] objects. */
  public companion object {
    /**
     * Convert a guest value to a [ReadableStreamSource].
     *
     * The [target] is expected to be a guest object with defined methods, as specified by the [ReadableStreamSource]
     * API. All methods are optional.
     *
     * @param target Value to convert
     * @return [ReadableStreamSource] which operates atop the developer's methods
     */
    @JvmStatic public fun from(target: Value): ReadableStreamSource =
      WebStreamsIntrinsic.GuestReadableSource.createFrom(target)
  }
}
