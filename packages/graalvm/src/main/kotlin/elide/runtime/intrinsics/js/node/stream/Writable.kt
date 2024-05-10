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
package elide.runtime.intrinsics.js.node.stream

import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.io.OutputStream
import elide.annotations.API
import elide.runtime.gvm.internals.node.stream.WrappedOutputStream
import elide.runtime.intrinsics.js.node.events.EventEmitter
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.vm.annotations.Polyglot

/**
 * # Node: Writable Stream.
 */
@HostAccess.Implementable
@API public interface Writable : StatefulStream, EventEmitter, EventTarget {
  /**
   * Is `true` if it is safe to call `writable.write()`, which means the stream has not been destroyed, errored, or
   * ended.
   */
  @get:Polyglot public val writable: Boolean

  /**
   * Returns whether the stream was destroyed or errored before emitting `'finish'`.
   */
  @get:Polyglot public val writableAborted: Boolean

  /**
   * Is `true` after `writable.end()` has been called. This property does not indicate whether the data has been
   * flushed, for this use `writable.writableFinished` instead.
   */
  @get:Polyglot public val writableEnded: Boolean

  /**
   * Number of times `writable.uncork()` needs to be called in order to fully uncork the stream.
   */
  @get:Polyglot public val writableCorked: Int

  /**
   * Returns error if the stream has been destroyed with an error.
   */
  @get:Polyglot public val errored: Any?

  /**
   * Is set to true immediately before the `'finish'` event is emitted.
   */
  @get:Polyglot public val writableFinished: Boolean

  /**
   * Return the value of `highWaterMark` passed when creating this Writable.
   */
  @get:Polyglot public val writableHighWaterMark: Int

  /**
   * This property contains the number of bytes (or objects) in the queue ready to be written. The value provides
   * introspection data regarding the status of the `highWaterMark`.
   */
  @get:Polyglot public val writableLength: Int

  /**
   * Is true if the stream's buffer has been full and stream will emit `'drain'`.
   */
  @get:Polyglot public val writableNeedDrain: Boolean

  /**
   * Getter for the property `objectMode` of a given `Writable` stream.
   */
  @get:Polyglot public val writableObjectMode: Boolean

  /**
   * The `writable.cork()` method forces all written data to be buffered in memory. The buffered data will be flushed
   * when either the `stream.uncork()` or `stream.end()` methods are called.
   *
   * The primary intent of `writable.cork()` is to accommodate a situation in which several small chunks are written to
   * the stream in rapid succession. Instead of immediately forwarding them to the underlying destination,
   * `writable.cork()` buffers all the chunks until `writable.uncork()` is called, which will pass them all to
   * `writable._writev()`, if present.
   *
   * This prevents a head-of-line blocking situation where data is being buffered while waiting for the first small
   * chunk to be processed. However, use of `writable.cork()` without implementing `writable._writev()` may have an
   * adverse effect on throughput.
   */
  @Polyglot public fun cork()

  /**
   * Destroy the stream. Optionally emit an `'error'` event, and emit a `'close'` event (unless `emitClose` is set to
   * `false`).
   *
   * After this call, the writable stream has ended and subsequent calls to `write()` or `end()` will result in an
   * `ERR_STREAM_DESTROYED` error. This is a destructive and immediate way to destroy a stream. Previous calls to
   * `write()` may not have drained, and may trigger an `ERR_STREAM_DESTROYED` error.
   *
   * Use `end()` instead of `destroy` if data should flush before close, or wait for the `'drain'` event before
   * destroying the stream.
   */
  @Polyglot override fun destroy()

  /**
   * Destroy the stream. Optionally emit an `'error'` event, and emit a `'close'` event (unless `emitClose` is set to
   * `false`).
   *
   * After this call, the writable stream has ended and subsequent calls to `write()` or `end()` will result in an
   * `ERR_STREAM_DESTROYED` error. This is a destructive and immediate way to destroy a stream. Previous calls to
   * `write()` may not have drained, and may trigger an `ERR_STREAM_DESTROYED` error.
   *
   * Use `end()` instead of `destroy` if data should flush before close, or wait for the `'drain'` event before
   * destroying the stream.
   *
   * @param error Error to set for this destruction
   */
  @Polyglot override fun destroy(error: Value)

  /**
   * Destroy the stream. Optionally emit an `'error'` event, and emit a `'close'` event (unless `emitClose` is set to
   * `false`).
   *
   * After this call, the writable stream has ended and subsequent calls to `write()` or `end()` will result in an
   * `ERR_STREAM_DESTROYED` error. This is a destructive and immediate way to destroy a stream. Previous calls to
   * `write()` may not have drained, and may trigger an `ERR_STREAM_DESTROYED` error.
   *
   * Use `end()` instead of `destroy` if data should flush before close, or wait for the `'drain'` event before
   * destroying the stream.
   *
   * @param error Error to set for this destruction
   */
  @Polyglot override fun destroy(error: Throwable)

  /**
   * The `writable.write()` method writes some data to the stream, and calls the supplied callback once the data has
   * been fully handled. If an error occurs, the callback will be called with the error as its first argument. The
   * callback is called asynchronously and before `'error'` is emitted.
   *
   * The return value is `true` if the internal buffer is less than the `highWaterMark` configured when the stream was
   * created after admitting chunk. If `false` is returned, further attempts to write data to the stream should stop
   * until the `'drain'` event is emitted.
   *
   * While a stream is not draining, calls to `write()` will buffer chunk, and return `false`. Once all currently
   * buffered chunks are drained (accepted for delivery by the operating system), the `'drain'` event will be emitted.
   * Once `write()` returns false, do not write more chunks until the `'drain'` event is emitted.
   *
   * While calling `write()` on a stream that is not draining is allowed, Node.js will buffer all written chunks until
   * maximum memory usage occurs, at which point it will abort unconditionally. Even before it aborts, high memory usage
   * will cause poor garbage collector performance and high RSS (which is not typically released back to the system,
   * even after the memory is no longer required). Since TCP sockets may never drain if the remote peer does not read
   * the data, writing a socket that is not draining may lead to a remotely exploitable vulnerability.
   *
   * Writing data while the stream is not draining is particularly problematic for a `Transform`, because the
   * `Transform` streams are paused by default until they are piped or a `'data'` or `'readable'` event handler is
   * added.
   *
   * If the data to be written can be generated or fetched on demand, it is recommended to encapsulate the logic into a
   * `Readable` and use `stream.pipe()`. However, if calling `write()` is preferred, it is possible to respect
   * backpressure and avoid memory issues using the `'drain'` event:
   *
   * ```javascript
   * function write(data, cb) {
   *   if (!stream.write(data)) {
   *     stream.once('drain', cb);
   *   } else {
   *     process.nextTick(cb);
   *   }
   * }
   *
   * // Wait for cb to be called before doing any other write.
   * write('hello', () => {
   *   console.log('Write completed, do more writes now.');
   * });
   * ```
   *
   * A `Writable` stream in object mode will always ignore the encoding argument.
   *
   * @param chunk Chunk of data to write to the stream
   */
  @Polyglot public fun write(chunk: Any?)

  /**
   * The `writable.write()` method writes some data to the stream, and calls the supplied callback once the data has
   * been fully handled. If an error occurs, the callback will be called with the error as its first argument. The
   * callback is called asynchronously and before `'error'` is emitted.
   *
   * The return value is `true` if the internal buffer is less than the `highWaterMark` configured when the stream was
   * created after admitting chunk. If `false` is returned, further attempts to write data to the stream should stop
   * until the `'drain'` event is emitted.
   *
   * While a stream is not draining, calls to `write()` will buffer chunk, and return `false`. Once all currently
   * buffered chunks are drained (accepted for delivery by the operating system), the `'drain'` event will be emitted.
   * Once `write()` returns false, do not write more chunks until the `'drain'` event is emitted.
   *
   * While calling `write()` on a stream that is not draining is allowed, Node.js will buffer all written chunks until
   * maximum memory usage occurs, at which point it will abort unconditionally. Even before it aborts, high memory usage
   * will cause poor garbage collector performance and high RSS (which is not typically released back to the system,
   * even after the memory is no longer required). Since TCP sockets may never drain if the remote peer does not read
   * the data, writing a socket that is not draining may lead to a remotely exploitable vulnerability.
   *
   * Writing data while the stream is not draining is particularly problematic for a `Transform`, because the
   * `Transform` streams are paused by default until they are piped or a `'data'` or `'readable'` event handler is
   * added.
   *
   * If the data to be written can be generated or fetched on demand, it is recommended to encapsulate the logic into a
   * `Readable` and use `stream.pipe()`. However, if calling `write()` is preferred, it is possible to respect
   * backpressure and avoid memory issues using the `'drain'` event:
   *
   * ```javascript
   * function write(data, cb) {
   *   if (!stream.write(data)) {
   *     stream.once('drain', cb);
   *   } else {
   *     process.nextTick(cb);
   *   }
   * }
   *
   * // Wait for cb to be called before doing any other write.
   * write('hello', () => {
   *   console.log('Write completed, do more writes now.');
   * });
   * ```
   *
   * A `Writable` stream in object mode will always ignore the encoding argument.
   *
   * @param chunk Chunk of data to write to the stream
   * @param encoding The encoding of the chunk
   */
  @Polyglot public fun write(chunk: StringOrBufferOrAny?, encoding: Value?)

  /**
   * The `writable.write()` method writes some data to the stream, and calls the supplied callback once the data has
   * been fully handled. If an error occurs, the callback will be called with the error as its first argument. The
   * callback is called asynchronously and before `'error'` is emitted.
   *
   * The return value is `true` if the internal buffer is less than the `highWaterMark` configured when the stream was
   * created after admitting chunk. If `false` is returned, further attempts to write data to the stream should stop
   * until the `'drain'` event is emitted.
   *
   * While a stream is not draining, calls to `write()` will buffer chunk, and return `false`. Once all currently
   * buffered chunks are drained (accepted for delivery by the operating system), the `'drain'` event will be emitted.
   * Once `write()` returns false, do not write more chunks until the `'drain'` event is emitted.
   *
   * While calling `write()` on a stream that is not draining is allowed, Node.js will buffer all written chunks until
   * maximum memory usage occurs, at which point it will abort unconditionally. Even before it aborts, high memory usage
   * will cause poor garbage collector performance and high RSS (which is not typically released back to the system,
   * even after the memory is no longer required). Since TCP sockets may never drain if the remote peer does not read
   * the data, writing a socket that is not draining may lead to a remotely exploitable vulnerability.
   *
   * Writing data while the stream is not draining is particularly problematic for a `Transform`, because the
   * `Transform` streams are paused by default until they are piped or a `'data'` or `'readable'` event handler is
   * added.
   *
   * If the data to be written can be generated or fetched on demand, it is recommended to encapsulate the logic into a
   * `Readable` and use `stream.pipe()`. However, if calling `write()` is preferred, it is possible to respect
   * backpressure and avoid memory issues using the `'drain'` event:
   *
   * ```javascript
   * function write(data, cb) {
   *   if (!stream.write(data)) {
   *     stream.once('drain', cb);
   *   } else {
   *     process.nextTick(cb);
   *   }
   * }
   *
   * // Wait for cb to be called before doing any other write.
   * write('hello', () => {
   *   console.log('Write completed, do more writes now.');
   * });
   * ```
   *
   * A `Writable` stream in object mode will always ignore the encoding argument.
   *
   * @param chunk Chunk of data to write to the stream
   * @param encoding The encoding of the chunk
   * @param callback The callback to call once the data has been written and flushed
   */
  @Polyglot public fun write(chunk: StringOrBufferOrAny?, encoding: Value?, callback: Value?)

  /**
   * The `writable.write()` method writes some data to the stream, and calls the supplied callback once the data has
   * been fully handled. If an error occurs, the callback will be called with the error as its first argument. The
   * callback is called asynchronously and before `'error'` is emitted.
   *
   * The return value is `true` if the internal buffer is less than the `highWaterMark` configured when the stream was
   * created after admitting chunk. If `false` is returned, further attempts to write data to the stream should stop
   * until the `'drain'` event is emitted.
   *
   * While a stream is not draining, calls to `write()` will buffer chunk, and return `false`. Once all currently
   * buffered chunks are drained (accepted for delivery by the operating system), the `'drain'` event will be emitted.
   * Once `write()` returns false, do not write more chunks until the `'drain'` event is emitted.
   *
   * While calling `write()` on a stream that is not draining is allowed, Node.js will buffer all written chunks until
   * maximum memory usage occurs, at which point it will abort unconditionally. Even before it aborts, high memory usage
   * will cause poor garbage collector performance and high RSS (which is not typically released back to the system,
   * even after the memory is no longer required). Since TCP sockets may never drain if the remote peer does not read
   * the data, writing a socket that is not draining may lead to a remotely exploitable vulnerability.
   *
   * Writing data while the stream is not draining is particularly problematic for a `Transform`, because the
   * `Transform` streams are paused by default until they are piped or a `'data'` or `'readable'` event handler is
   * added.
   *
   * If the data to be written can be generated or fetched on demand, it is recommended to encapsulate the logic into a
   * `Readable` and use `stream.pipe()`. However, if calling `write()` is preferred, it is possible to respect
   * backpressure and avoid memory issues using the `'drain'` event:
   *
   * ```javascript
   * function write(data, cb) {
   *   if (!stream.write(data)) {
   *     stream.once('drain', cb);
   *   } else {
   *     process.nextTick(cb);
   *   }
   * }
   *
   * // Wait for cb to be called before doing any other write.
   * write('hello', () => {
   *   console.log('Write completed, do more writes now.');
   * });
   * ```
   *
   * A `Writable` stream in object mode will always ignore the encoding argument.
   *
   * @param chunk Chunk of data to write to the stream
   * @param encoding The encoding of the chunk
   * @param callback The callback to call once the data has been written and flushed
   */
  @Polyglot public fun write(chunk: StringOrBufferOrAny?, encoding: Value? = null, callback: () -> Unit)

  /**
   * Calling the `writable.end()` method signals that no more data will be written to the [Writable]. The optional
   * `chunk` and `encoding` arguments allow one final additional chunk of data to be written immediately before closing
   * the stream.
   *
   * Calling the [write] method after calling `stream.end()` will raise an error.
   */
  @Polyglot public fun end()

  /**
   * Calling the `writable.end()` method signals that no more data will be written to the [Writable]. The optional
   * [chunk] and [encoding] arguments allow one final additional chunk of data to be written immediately before closing
   * the stream.
   *
   * Calling the [write] method after calling `stream.end()` will raise an error.
   *
   * @param chunk The final chunk of data to write to the stream
   * @param encoding The encoding of the chunk
   */
  @Polyglot public fun end(chunk: StringOrBufferOrAny?, encoding: Value?)

  /**
   * Calling the `writable.end()` method signals that no more data will be written to the [Writable]. The optional
   * [chunk] and [encoding] arguments allow one final additional chunk of data to be written immediately before closing
   * the stream.
   *
   * Calling the [write] method after calling `stream.end()` will raise an error.
   *
   * @param chunk The final chunk of data to write to the stream
   * @param encoding The encoding of the chunk
   * @param callback The callback to call once the data has been written and flushed
   */
  @Polyglot public fun end(chunk: StringOrBufferOrAny?, encoding: Value?, callback: Value?)

  /**
   * Calling the `writable.end()` method signals that no more data will be written to the [Writable]. The optional
   * [chunk] and [encoding] arguments allow one final additional chunk of data to be written immediately before closing
   * the stream.
   *
   * Calling the [write] method after calling `stream.end()` will raise an error.
   *
   * @param chunk The final chunk of data to write to the stream
   * @param encoding The encoding of the chunk
   * @param callback The callback to call once the data has been written and flushed
   */
  @Polyglot public fun end(chunk: StringOrBufferOrAny?, encoding: Value? = null, callback: () -> Unit)

  /** Factory methods for creating [Writable] instances. */
  public companion object {
    /**
     * Wrap the provided [stream] (a regular [OutputStream]) as a [Writable].
     *
     * @param stream The stream to wrap
     * @return The wrapped stream
     */
    @JvmStatic public fun wrap(stream: OutputStream): Writable = WrappedOutputStream.wrap(stream)
  }
}
