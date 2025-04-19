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
package elide.runtime.intrinsics.js

/**
 * # Stream
 *
 * Marker interface for Web Streams types, including [ReadableStream], [WritableStream], and [TransformStream]. The Web
 * Streams API provides a standard way to handle streaming data in the browser and on the server.
 *
 * From MDN:
 * The Streams API allows JavaScript to programmatically access streams of data received over the network and process
 * them as desired by the developer.
 *
 * ## Concepts and usage
 *
 * Streaming involves breaking a resource that you want to receive over a network down into small chunks, then
 * processing it bit by bit. Browsers already do this when receiving media assets — videos buffer and play as more of
 * the content downloads, and sometimes you'll see images display gradually as more is loaded too.
 *
 * But this capability has never been available to JavaScript before. Previously, if we wanted to process a resource of
 * some kind (video, text file, etc.), we'd have to download the entire file, wait for it to be deserialized into a
 * suitable format, then process all the data.
 *
 * With the Streams API, you can start processing raw data with JavaScript bit by bit, as soon as it is available,
 * without needing to generate a buffer, string, or blob.
 *
 * There are more advantages too — you can detect when streams start or end, chain streams together, handle errors and
 * cancel streams as required, and react to the speed at which the stream is being read.
 *
 * The usage of Streams hinges on making responses available as streams. For example, the response body returned by a
 * successful `fetch` request is a [ReadableStream] that can be read by a reader created with
 * [ReadableStream.getReader].
 *
 * More complicated uses involve creating your own stream using the [ReadableStream] constructor
 * ([ReadableStream.Factory]), for example to process data inside a service worker.
 *
 * You can also write data to streams using [WritableStream].
 *
 * ## Stream Interfaces
 *
 * This section describes each type of stream which is constituent to the Web Streams API.
 *
 * ### Readable streams
 *
 * [ReadableStream]
 * Represents a readable stream of data. It can be used to handle response streams of the Fetch API, or
 * developer-defined streams (e.g. a custom [ReadableStream] constructor).
 *
 * [ReadableStreamDefaultReader]
 * Represents a default reader that can be used to read stream data supplied from a network (e.g. a fetch request).
 *
 * [ReadableStreamDefaultController]
 * Represents a controller allowing control of a ReadableStream's state and internal queue. Default controllers are for
 * streams that are not byte streams.
 *
 * ### Writable streams
 *
 * [WritableStream]
 * Provides a standard abstraction for writing streaming data to a destination, known as a sink. This object comes with
 * built-in backpressure and queuing.
 *
 * [WritableStreamDefaultWriter]
 * Represents a default writable stream writer that can be used to write chunks of data to a writable stream.
 *
 * [WritableStreamDefaultController]
 * Represents a controller allowing control of a WritableStream's state. When constructing a WritableStream, the
 * underlying sink is given a corresponding WritableStreamDefaultController instance to manipulate.
 *
 * ### Transform streams
 *
 * [TransformStream]
 * Represents an abstraction for a stream object that transforms data as it passes through a pipe chain of stream
 * objects.
 *
 * [TransformStreamDefaultController]
 * Provides methods to manipulate the [ReadableStream] and [WritableStream] associated with a transform stream.
 *
 * ## Related stream APIs and operations
 *
 * [ByteLengthQueuingStrategy]
 * Provides a built-in byte length queuing strategy that can be used when constructing streams.
 *
 * [CountQueuingStrategy]
 * Provides a built-in chunk counting queuing strategy that can be used when constructing streams.
 *
 * ## ByteStream-related interfaces
 *
 * [ReadableStreamBYOBReader]
 * Represents a BYOB ("bring your own buffer") reader that can be used to read stream data supplied by the developer
 * (e.g. a custom [ReadableStream] constructor).
 *
 * [ReadableByteStreamController]
 * Represents a controller allowing control of a ReadableStream's state and internal queue. Byte stream controllers are
 * for byte streams.
 *
 * [ReadableStreamBYOBRequest]
 * Represents a pull into request in a [ReadableByteStreamController].
 *
 * @see ReadableStream Readable Stream API
 * @see WritableStream Writable Stream API
 * @see TransformStream Transform Stream API
 */
public sealed interface Stream
