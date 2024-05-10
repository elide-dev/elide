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
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.InputStream
import java.nio.charset.Charset
import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.node.stream.WrappedInputStream
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.events.EventEmitter
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.vm.annotations.Polyglot

// All properties made available via `Readable`.
private val READABLE_PROPS_AND_METHODS = arrayOf(
  "closed",
  "destroyed",
  "readable",
  "readableAborted",
  "readableDidRead",
  "readableEncoding",
  "readableEnded",
  "errored",
  "readableFlowing",
  "readableHighWaterMark",
  "readableLength",
  "readableObjectMode",
  "destroy",
  "isPaused",
  "pause",
  "pipe",
  "read",
  "resume",
  "setEncoding",
  "unpipe",
  "unshift",
  "wrap",
  "compose",
  "iterator",
  "map",
  "filter",
  "forEach",
  "toArray",
  "some",
  "find",
  "every",
  "flatMap",
  "drop",
  "take",
  "reduce"
)

// All properties made available via the class `Readable`.
private val READABLE_STATIC_PROPS_AND_METHODS = arrayOf(
  "from",
)

/**
 * # Node: Readable Stream.
 *
 * A Readable stream is an abstraction for a source from which data is consumed; an example of a Readable stream is the
 * response object returned by `http.get()`, or the input stream provided to a running program.
 *
 * Examples of Readable streams include:
 *
 * - HTTP responses, on the client
 * - HTTP requests, on the server
 * - fs read streams
 * - zlib streams
 * - crypto streams
 * - TCP sockets
 * - child process `stdout` and `stderr`
 * - `process.stdin`
 *
 * All Readable streams implement the interface defined by the stream.Readable class.
 *
 * &nbsp;
 *
 * ## Two reading modes
 *
 * [Readable] streams effectively operate in one of two modes: flowing and paused. These modes are separate from
 * [object mode](https://nodejs.org/api/stream.html#object-mode). A [Readable] stream can be in object mode or not,
 * regardless of whether it is in flowing mode or paused mode.
 *
 * In flowing mode, data is read from the underlying system automatically and provided to an application as quickly as
 * possible using events via the [EventEmitter] interface.
 *
 * In paused mode, the [read] method must be called explicitly to read chunks of data from the stream.
 *
 * All [Readable] streams begin in paused mode but can be switched to flowing mode in one of the following ways:
 *
 * - Adding a `'data'` event handler.
 * - Calling the [resume] method.
 * - Calling the [pipe] method to send the data to a [Writable].
 * - The [Readable] can switch back to paused mode using one of the following:
 *   - If there are no pipe destinations, by calling the [pause] method.
 *   - If there are pipe destinations, by removing all pipe destinations. Multiple pipe destinations may be removed by
 *     calling the [unpipe] method.
 *
 * The important concept to remember is that a [Readable] will not generate data until a mechanism for either consuming
 * or ignoring that data is provided. If the consuming mechanism is disabled or taken away, the [Readable] will attempt
 * to stop generating the data.
 *
 * For backward compatibility reasons, removing `'data'` event handlers will not automatically pause the stream. Also,
 * if there are piped destinations, then calling [pause] will not guarantee that the stream will remain paused once
 * those destinations drain and ask for more data.
 *
 * If a Readable is switched into flowing mode and there are no consumers available to handle the data, that data will
 * be lost. This can occur, for instance, when the [resume] method is called without a listener attached to the `'data'`
 * event, or when a `'data'` event handler is removed from the stream.
 *
 * Adding a `'readable'` event handler automatically makes the stream stop flowing, and the data has to be consumed via
 * [read]. If the `'readable'` event handler is removed, then the stream will start flowing again if there is a `'data'`
 * event handler.
 *
 * &nbsp;
 *
 * ## Three states
 *
 * The "two modes" of operation for a [Readable] stream are a simplified abstraction for the more complicated internal
 * state management that is happening within the [Readable] stream implementation.
 *
 * Specifically, at any given point in time, every [Readable] is in one of three possible states:
 *
 * - `readable.readableFlowing === null`
 * - `readable.readableFlowing === false`
 * - `readable.readableFlowing === true`
 *
 * When [Readable.readableFlowing] is `null`, no mechanism for consuming the stream's data is provided. Therefore, the
 * stream will not generate data. While in this state, attaching a listener for the `'data'` event, calling the
 * [Readable.pipe] method, or calling the [Readable.resume] method will switch [Readable.readableFlowing] to `true`,
 * causing the [Readable] to begin actively emitting events as data is generated.
 *
 * Calling [Readable.pause], [Readable.unpipe], or receiving backpressure will cause the [Readable.readableFlowing]
 * flag to be set as `false`, temporarily halting the flowing of events but not halting the generation of data. While in
 * this state, attaching a listener for the `'data'` event will not switch [Readable.readableFlowing] to `true`.
 *
 * &nbsp;
 *
 * ## Event: 'close'
 *
 * The `'close'` event is emitted when the stream and any of its underlying resources (a file descriptor, for example)
 * have been closed. The event indicates that no more events will be emitted, and no further computation will occur.
 *
 * @see Writable for the corresponding writable stream interface.
 */
@HostAccess.Implementable
@API public interface Readable : StatefulStream, EventEmitter, EventTarget, ProxyObject, ProxyIterable {
  /**
   * Is true if it is safe to call `readable.read()`, which means the stream has not been destroyed or emitted `'error'`
   * or `'end'`.
   */
  @get:Polyglot public val readable: Boolean

  /**
   * Returns whether the stream was destroyed or errored before emitting `'end'`.
   */
  @get:Polyglot public val readableAborted: Boolean

  /**
   * Returns whether `'data'` has been emitted.
   */
  @get:Polyglot public val readableDidRead: Boolean

  /**
   * Getter for the property `encoding` of a given `Readable` stream. The encoding property can be set using the
   * `readable.setEncoding()` method.
   */
  @get:Polyglot public val readableEncoding: String?

  /**
   * Becomes true when `'end'` event is emitted.
   */
  @get:Polyglot public val readableEnded: Boolean

  /**
   * Returns error if the stream has been destroyed with an error.
   */
  @get:Polyglot public val errored: Any?

  /**
   * This property reflects the current state of a Readable stream as described in the "Three states" section.
   */
  @get:Polyglot public val readableFlowing: Boolean?

  /**
   * Returns the value of `highWaterMark` passed when creating this `Readable`.
   */
  @get:Polyglot public val readableHighWaterMark: Int

  /**
   * This property contains the number of bytes (or objects) in the queue ready to be read. The value provides
   * introspection data regarding the status of the `highWaterMark`.
   */
  @get:Polyglot public val readableLength: Int

  /**
   * Getter for the property `objectMode` of a given `Readable` stream.
   */
  @get:Polyglot public val readableObjectMode: Boolean

  /**
   * Destroy the stream. Optionally emit an `error` event, and emit a `close` event (unless emitClose is set to false).
   * After this call, the readable stream will release any internal resources and subsequent calls to `push()` will be
   * ignored.
   *
   * Once `destroy()` has been called any further calls will be a no-op and no further errors except from `_destroy()`
   * may be emitted as `error`.
   *
   * Implementors should not override this method, but instead implement `readable._destroy()`.
   */
  @Polyglot override fun destroy()

  /**
   * Destroy the stream. Optionally emit an `error` event, and emit a `close` event (unless emitClose is set to false).
   * After this call, the readable stream will release any internal resources and subsequent calls to `push()` will be
   * ignored.
   *
   * Once `destroy()` has been called any further calls will be a no-op and no further errors except from `_destroy()`
   * may be emitted as `error`.
   *
   * Implementors should not override this method, but instead implement `readable._destroy()`.
   *
   * @param error The error, if any, that caused the stream to be destroyed.
   */
  @Polyglot override fun destroy(error: Value)

  /**
   * Destroy the stream. Optionally emit an `error` event, and emit a `close` event (unless emitClose is set to false).
   * After this call, the readable stream will release any internal resources and subsequent calls to `push()` will be
   * ignored.
   *
   * Once `destroy()` has been called any further calls will be a no-op and no further errors except from `_destroy()`
   * may be emitted as `error`.
   *
   * Implementors should not override this method, but instead implement `readable._destroy()`.
   *
   * @param error The error, if any, that caused the stream to be destroyed.
   */
  @Polyglot override fun destroy(error: Throwable)

  /**
   * The `readable.isPaused()` method returns the current operating state of the Readable. This is used primarily by the
   * mechanism that underlies the `readable.pipe()` method. In most typical cases, there will be no reason to use this
   * method directly.
   *
   * @return `true` if the Readable is in a paused state, `false` otherwise.
   */
  @Polyglot public fun isPaused(): Boolean

  /**
   * The `readable.pause()` method will cause a stream in flowing mode to stop emitting `data` events, switching out of
   * flowing mode. Any data that becomes available will remain in the internal buffer.
   *
   * This method is primarily used in conjunction with the `readable.resume()` method to manage stream flow.
   *
   * @return The Readable stream.
   */
  @Polyglot public fun pause(): Readable

  /**
   * The `readable.pipe()` method attaches a Writable stream to the readable, causing it to switch into flowing mode.
   *
   * Data is read from the Readable source and passed to the supplied Writable destination. The destination is generally
   * a Writable stream, but may be any object that implements the `Writable` stream interface.
   *
   * The `options` argument is an optional object that may contain the following properties:
   *
   * - `end` - A boolean that specifies whether or not the `destination` should be automatically closed when the
   *   `source` ends. Default: `true`.
   *
   * This method variant takes only a [destination]; see other variants for additional options.
   *
   * @param destination The destination for writing data.
   */
  @Polyglot public fun pipe(destination: Writable)

  /**
   * The `readable.pipe()` method attaches a Writable stream to the readable, causing it to switch into flowing mode.
   *
   * Data is read from the Readable source and passed to the supplied Writable destination. The destination is generally
   * a Writable stream, but may be any object that implements the `Writable` stream interface.
   *
   * The `options` argument is an optional object that may contain the following properties:
   *
   * - `end` - A boolean that specifies whether or not the `destination` should be automatically closed when the
   *   `source` ends. Default: `true`.
   *
   * This method variant takes a [destination] and [options], expressed as a guest [Value].
   *
   * @param destination The destination for writing data.
   * @param options The options for the pipe operation.
   */
  @Polyglot public fun pipe(destination: Writable, options: Value)

  /**
   * The `readable.pipe()` method attaches a Writable stream to the readable, causing it to switch into flowing mode.
   *
   * Data is read from the Readable source and passed to the supplied Writable destination. The destination is generally
   * a Writable stream, but may be any object that implements the `Writable` stream interface.
   *
   * The `options` argument is an optional object that may contain the following properties:
   *
   * - `end` - A boolean that specifies whether or not the `destination` should be automatically closed when the
   *   `source` ends. Default: `true`.
   *
   * This method variant takes a [destination] and [options], expressed as a [ReadablePipeOptions].
   *
   * @param destination The destination for writing data.
   * @param options The options for the pipe operation.
   */
  @Polyglot public fun pipe(destination: Writable, options: ReadablePipeOptions)

  /**
   * The `readable.read()` method reads some data from the readable stream and returns it. The data will not be returned
   * immediately if the readable stream is in flowing mode; in that case, the data will be buffered internally, and this
   * method will return it when it is available.
   *
   * If the stream has ended, `null` will be returned. If there is no data available, `null` will be returned.
   *
   * @return The data read from the stream.
   */
  @Polyglot public fun read(): StringOrBufferOrAny

  /**
   * The `readable.read()` method reads some data from the readable stream and returns it. The data will not be returned
   * immediately if the readable stream is in flowing mode; in that case, the data will be buffered internally, and this
   * method will return it when it is available.
   *
   * If the stream has ended, `null` will be returned. If there is no data available, `null` will be returned.
   *
   * @param size The number of bytes to read. If not specified, all available data is read.
   * @return The data read from the stream.
   */
  @Polyglot public fun read(size: Value): StringOrBufferOrAny

  /**
   * The `readable.read()` method reads some data from the readable stream and returns it. The data will not be returned
   * immediately if the readable stream is in flowing mode; in that case, the data will be buffered internally, and this
   * method will return it when it is available.
   *
   * If the stream has ended, `null` will be returned. If there is no data available, `null` will be returned.
   *
   * @param size The number of bytes to read. If not specified, all available data is read.
   * @return The data read from the stream.
   */
  @Polyglot public fun read(size: Int): StringOrBufferOrAny

  /**
   * The `readable.resume()` method will cause a stream in paused mode to switch back into flowing mode. This will cause
   * the stream to start emitting `data` events.
   *
   * This method is primarily used in conjunction with the `readable.pause()` method to manage stream flow.
   *
   * @return The Readable stream.
   */
  @Polyglot public fun resume(): Readable

  /**
   * The `readable.setEncoding()` method sets the encoding for the Readable stream. This method must be called before
   * the first `'data'` event is emitted by the stream.
   *
   * @param encoding The encoding to use.
   */
  @Polyglot public fun setEncoding(encoding: String)

  /**
   * The `readable.unpipe()` method detaches a Writable stream previously attached using `readable.pipe()`. The method
   * will remove the destination from the list of destinations to which data will be written.
   *
   * If no `destination` is specified, then all attached Writable streams will be detached.
   */
  @Polyglot public fun unpipe()

  /**
   * The `readable.unpipe()` method detaches a Writable stream previously attached using `readable.pipe()`. The method
   * will remove the destination from the list of destinations to which data will be written.
   *
   * If no `destination` is specified, then all attached Writable streams will be detached.
   *
   * @param destination The destination to unpipe.
   */
  @Polyglot public fun unpipe(destination: Writable)

  /**
   * The `readable.unshift()` method pushes a chunk of data back into the internal buffer for the stream.
   *
   * This is useful in certain cases where a stream is being consumed by a parser, which needs to "un-consume" some data
   * that it has optimistically pulled out of the source, so that it can be processed by a different parser.
   *
   * @param chunk The chunk of data to unshift.
   */
  @Polyglot public fun unshift(chunk: Value)

  /**
   * The `readable.unshift()` method pushes a chunk of data back into the internal buffer for the stream.
   *
   * This is useful in certain cases where a stream is being consumed by a parser, which needs to "un-consume" some data
   * that it has optimistically pulled out of the source, so that it can be processed by a different parser.
   *
   * @param chunk The chunk of data to unshift.
   */
  @Polyglot public fun unshift(chunk: StreamChunk)

  /**
   * The `readable.unshift()` method pushes a chunk of data back into the internal buffer for the stream.
   *
   * This is useful in certain cases where a stream is being consumed by a parser, which needs to "un-consume" some data
   * that it has optimistically pulled out of the source, so that it can be processed by a different parser.
   *
   * @param chunk The chunk of data to unshift.
   * @param encoding The encoding of the chunk.
   */
  @Polyglot public fun unshift(chunk: StreamChunk, encoding: String)

  /**
   * The `readable.wrap()` method is used to wrap a raw stream source (like a TCP socket) in a Readable stream
   * interface.
   *
   * @param stream The raw ("old-style") stream source to wrap.
   */
  @Polyglot public fun wrap(stream: Stream)

  /**
   * Compose this stream with another one, to produce a duplex stream.
   *
   * @param stream The stream to compose with.
   * @return The duplex stream.
   */
  @Polyglot public fun compose(stream: Stream): Duplex

  /**
   * Compose this stream with another one, to produce a duplex stream.
   *
   * @param stream The stream to compose with.
   * @param options The options for the compose operation.
   * @return The duplex stream.
   */
  @Polyglot public fun compose(stream: Stream, options: Value): Duplex

  /**
   * Compose this stream with another one, to produce a duplex stream.
   *
   * @param stream The stream to compose with.
   * @param options The options for the compose operation.
   * @return The duplex stream.
   */
  @Polyglot public fun compose(stream: Stream, options: ReadableComposeOptions): Duplex

  /**
   * Get an iterator for this stream.
   *
   * @return The iterator.
   */
  @Polyglot public fun iterator()

  /**
   * Get an iterator for this stream.
   *
   * @param options The options for the iterator.
   * @return The iterator.
   */
  @Polyglot public fun iterator(options: Value)

  /**
   * Get an iterator for this stream.
   *
   * @param options The options for the iterator.
   * @return The iterator.
   */
  @Polyglot public fun iterator(options: ReadableIteratorOptions)

  /**
   * Map over this stream to produce results from a new [Readable].
   *
   * @param cbk The callback to map with.
   * @return The mapped stream.
   */
  @Polyglot public fun map(cbk: Value): Readable

  /**
   * Map over this stream to produce results from a new [Readable].
   *
   * @param cbk The callback to map with.
   * @param options The options for the map operation.
   * @return The mapped stream.
   */
  @Polyglot public fun map(cbk: Value, options: Value): Readable

  /**
   * Map over this stream to produce results from a new [Readable].
   *
   * @param cbk The callback to map with.
   * @return The mapped stream.
   */
  @Polyglot public fun map(cbk: (Value) -> Value): Readable

  /**
   * Map over this stream to produce results from a new [Readable].
   *
   * @param options The options for the map operation.
   * @param cbk The callback to map with.
   * @return The mapped stream.
   */
  @Polyglot public fun map(options: ReadableMapOptions, cbk: (Any) -> Value): Readable

  /**
   * Filter this stream to produce results from a new [Readable].
   *
   * @param cbk The callback to filter with.
   * @return The filtered stream.
   */
  @Polyglot public fun filter(cbk: Value): Readable

  /**
   * Filter this stream to produce results from a new [Readable].
   *
   * @param cbk The callback to filter with.
   * @param options The options for the filter operation.
   * @return The filtered stream.
   */
  @Polyglot public fun filter(cbk: Value, options: Value): Readable

  /**
   * Filter this stream to produce results from a new [Readable].
   *
   * @param cbk The callback to filter with.
   * @return The filtered stream.
   */
  @Polyglot public fun filter(cbk: (Value) -> Boolean): Readable

  /**
   * Filter this stream to produce results from a new [Readable].
   *
   * @param options The options for the filter operation.
   * @param cbk The callback to filter with.
   * @return The filtered stream.
   */
  @Polyglot public fun filter(options: ReadableMapOptions, cbk: (Value) -> Boolean): Readable

  /**
   * For each item in this stream, call the given callback.
   *
   * @param cbk The callback to call.
   * @return A promise that resolves when the operation is complete.
   */
  @Polyglot public fun forEach(cbk: Value): JsPromise<Unit>

  /**
   * For each item in this stream, call the given callback.
   *
   * @param cbk The callback to call.
   * @param options The options for the forEach operation.
   * @return A promise that resolves when the operation is complete.
   */
  @Polyglot public fun forEach(cbk: Value, options: Value): JsPromise<Unit>

  /**
   * For each item in this stream, call the given callback.
   *
   * @param cbk The callback to call.
   * @return A promise that resolves when the operation is complete.
   */
  @Polyglot public fun forEach(cbk: (Value) -> Unit): JsPromise<Unit>

  /**
   * For each item in this stream, call the given callback.
   *
   * @param options The options for the forEach operation.
   * @param cbk The callback to call.
   * @return A promise that resolves when the operation is complete.
   */
  @Polyglot public fun forEach(options: ReadableForEachOptions, cbk: (Value) -> Unit): JsPromise<Unit>

  /**
   * Collect all items in this stream into an array.
   *
   * @return A promise that resolves with the array of items.
   */
  @Polyglot public fun toArray(): JsPromise<Array<Value>>

  /**
   * Collect all items in this stream into an array.
   *
   * @param options The options for the toArray operation.
   * @return A promise that resolves with the array of items.
   */
  @Polyglot public fun toArray(options: Value): JsPromise<Array<Value>>

  /**
   * Collect all items in this stream into an array.
   *
   * @param options The options for the toArray operation.
   * @return A promise that resolves with the array of items.
   */
  @Polyglot public fun toArray(options: ReadableToArrayOptions): JsPromise<Array<Value>>

  /**
   * Check if any item in this stream matches the given predicate.
   *
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun some(cbk: Value): JsPromise<Boolean>

  /**
   * Check if any item in this stream matches the given predicate.
   *
   * @param cbk The predicate to check.
   * @param options The options for the some operation.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun some(cbk: Value, options: Value): JsPromise<Boolean>

  /**
   * Check if any item in this stream matches the given predicate.
   *
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun some(cbk: () -> Boolean?): JsPromise<Boolean>

  /**
   * Check if any item in this stream matches the given predicate.
   *
   * @param options The options for the some operation.
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun some(options: ReadableSomeOptions, cbk: () -> Boolean?): JsPromise<Boolean>

  /**
   * Find the first item in this stream that matches the given predicate.
   *
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun find(cbk: Value): JsPromise<Value>

  /**
   * Find the first item in this stream that matches the given predicate.
   *
   * @param cbk The predicate to check.
   * @param options The options for the find operation.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun find(cbk: Value, options: Value): JsPromise<Value>

  /**
   * Find the first item in this stream that matches the given predicate.
   *
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun find(cbk: () -> Boolean?): JsPromise<Value>

  /**
   * Find the first item in this stream that matches the given predicate.
   *
   * @param options The options for the find operation.
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun find(options: ReadableFindOptions, cbk: () -> Boolean?): JsPromise<Value>

  /**
   * Check if all items in this stream match the given predicate.
   *
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun every(cbk: Value): JsPromise<Boolean>

  /**
   * Check if all items in this stream match the given predicate.
   *
   * @param cbk The predicate to check.
   * @param options The options for the every operation.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun every(cbk: Value, options: Value): JsPromise<Boolean>

  /**
   * Check if all items in this stream match the given predicate.
   *
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun every(cbk: () -> Boolean?): JsPromise<Boolean>

  /**
   * Check if all items in this stream match the given predicate.
   *
   * @param options The options for the every operation.
   * @param cbk The predicate to check.
   * @return A promise that resolves with the result.
   */
  @Polyglot public fun every(options: ReadableEveryOptions, cbk: () -> Boolean?): JsPromise<Boolean>

  /**
   * Map over this stream to produce a new stream of items.
   *
   * @param cbk The callback to map with.
   * @return The mapped stream.
   */
  @Polyglot public fun flatMap(cbk: Value): Readable

  /**
   * Map over this stream to produce a new stream of items.
   *
   * @param cbk The callback to map with.
   * @param options The options for the flatMap operation.
   * @return The mapped stream.
   */
  @Polyglot public fun flatMap(cbk: Value, options: Value): Readable

  /**
   * Map over this stream to produce a new stream of items.
   *
   * @param cbk The callback to map with.
   * @return The mapped stream.
   */
  @Polyglot public fun flatMap(cbk: () -> Iterable<Value>): Readable

  /**
   * Map over this stream to produce a new stream of items.
   *
   * @param options The options for the flatMap operation.
   * @param cbk The callback to map with.
   * @return The mapped stream.
   */
  @Polyglot public fun flatMap(options: ReadableFlatMapOptions, cbk: () -> Iterable<Value>): Readable

  /**
   * Drop the first `limit` items from this stream.
   *
   * @param limit The number of items to drop.
   * @return The new stream.
   */
  @Polyglot public fun drop(limit: Int): Readable

  /**
   * Drop the first `limit` items from this stream.
   *
   * @param limit The number of items to drop.
   * @param options The options for the drop operation.
   * @return The new stream.
   */
  @Polyglot public fun drop(limit: Int, options: Value): Readable

  /**
   * Drop the first `limit` items from this stream.
   *
   * @param limit The number of items to drop.
   * @param options The options for the drop operation.
   * @return The new stream.
   */
  @Polyglot public fun drop(limit: Int, options: ReadableDropOptions): Readable

  /**
   * Take the first `limit` items from this stream.
   *
   * @param limit The number of items to take.
   * @return The new stream.
   */
  @Polyglot public fun take(limit: Int): Readable

  /**
   * Take the first `limit` items from this stream.
   *
   * @param limit The number of items to take.
   * @param options The options for the take operation.
   * @return The new stream.
   */
  @Polyglot public fun take(limit: Int, options: Value): Readable

  /**
   * Take the first `limit` items from this stream.
   *
   * @param limit The number of items to take.
   * @param options The options for the take operation.
   * @return The new stream.
   */
  @Polyglot public fun take(limit: Int, options: ReadableDropOptions): Readable

  /**
   * Reduce this stream to a single value.
   *
   * @param op The reduction operation.
   * @return The reduced value.
   */
  @Polyglot public fun reduce(op: Value): JsPromise<Value>

  /**
   * Reduce this stream to a single value.
   *
   * @param op The reduction operation.
   * @param initial The initial value.
   * @return The reduced value.
   */
  @Polyglot public fun reduce(op: Value, initial: Value): JsPromise<Value>

  /**
   * Reduce this stream to a single value.
   *
   * @param op The reduction operation.
   * @param initial The initial value.
   * @param options The options for the reduce operation.
   * @return The reduced value.
   */
  @Polyglot public fun reduce(op: Value, initial: Value, options: Value): JsPromise<Value>

  /**
   * Reduce this stream to a single value.
   *
   * @param op The reduction operation.
   * @return The reduced value.
   */
  @Polyglot public fun reduce(op: () -> Value): JsPromise<Value>

  /**
   * Reduce this stream to a single value.
   *
   * @param initial The initial value.
   * @param op The reduction operation.
   * @return The reduced value.
   */
  @Polyglot public fun reduce(initial: Value, op: () -> Value): JsPromise<Value>

  /**
   * Reduce this stream to a single value.
   *
   * @param initial The initial value.
   * @param op The reduction operation.
   * @param options The options for the reduce operation.
   * @return The reduced value.
   */
  @Polyglot public fun reduce(initial: Value, options: ReadableReduceOptions, op: () -> Value): JsPromise<Value>

  override fun getMemberKeys(): Array<String> = READABLE_PROPS_AND_METHODS
  override fun hasMember(key: String): Boolean = key in READABLE_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot mutate arbitrary members on `Readable`")
  }

  override fun removeMember(key: String?): Boolean {
    throw UnsupportedOperationException("Cannot mutate arbitrary members on `Readable`")
  }

  /**
   * ## Node Readable: Constructors
   *
   * Describes the type layout of static methods which are made available on the [Readable] class.
   */
  @API public interface Constructors : ProxyInstantiable, ProxyObject {
    /**
     * Create a new [Readable] stream from the given iterable.
     *
     * @param iterable The iterable to create the stream from.
     * @return The new readable stream.
     */
    @Polyglot public fun from(iterable: Value): Readable

    /**
     * Create a new [Readable] stream from the given iterable.
     *
     * @param iterable The iterable to create the stream from.
     * @param options The options for the stream.
     * @return The new readable stream.
     */
    @Polyglot public fun from(iterable: Value, options: Value): Readable

    /**
     * Create a new [Readable] stream from the given iterable.
     *
     * @param iterable The iterable to create the stream from.
     * @param options The options for the stream.
     * @return The new readable stream.
     */
    @Polyglot public fun from(iterable: Iterable<*>, options: ReadableFromOptions): Readable

    override fun getMemberKeys(): Array<String> = READABLE_STATIC_PROPS_AND_METHODS
    override fun hasMember(key: String): Boolean = key in READABLE_STATIC_PROPS_AND_METHODS

    override fun putMember(key: String, value: Value) {
      throw UnsupportedOperationException("Cannot mutate arbitrary members on `Readable`")
    }

    override fun removeMember(key: String): Boolean {
      throw UnsupportedOperationException("Cannot mutate arbitrary members on `Readable`")
    }

    override fun getMember(key: String): Any? = when (key) {
      "from" -> ProxyExecutable {
        val iterable = it.getOrNull(0)
          ?: throw JsError.valueError("Expected an iterable as the first argument to `Readable.from`")
        val options = it.getOrNull(1)
        if (options != null) from(iterable, options) else from(iterable)
      }
      else -> null
    }
  }

  /** Factory methods for creating [Readable] streams. */
  public companion object : Constructors {
    /**
     * Wrap the provided [InputStream] as a [Readable].
     *
     * @param stream The input stream to wrap.
     * @return The wrapped readable stream.
     */
    @JvmStatic public fun wrap(stream: InputStream, encoding: Charset? = null): Readable =
      WrappedInputStream.wrap(stream, encoding)

    @Polyglot override fun from(iterable: Value): Readable {
      TODO("Not yet implemented")
    }

    @Polyglot override fun from(iterable: Value, options: Value): Readable {
      TODO("Not yet implemented")
    }

    @Polyglot override fun from(iterable: Iterable<*>, options: ReadableFromOptions): Readable {
      TODO("Not yet implemented")
    }

    override fun newInstance(vararg arguments: Value?): Any {
      TODO("Not yet implemented")
    }
  }
}
