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

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import java.nio.charset.Charset

/** Returns the string value of a header with the given [name] in this request, if it exists. */
public fun HttpRequest.getHeader(name: CharSequence): String? = headers().get(name)

/** Returns all values of a header with the given [name] in this request. */
public fun HttpRequest.getHeaders(name: CharSequence): List<String> = headers().getAll(name).orEmpty()

/** Adds a [value] to the header with the given [name] for this request. */
public fun HttpResponse.addHeader(name: CharSequence, value: Any): HttpResponse = apply { headers().add(name, value) }

/** Adds a group of [values] to the header with the given [name] for this request. */
public fun HttpResponse.addHeaders(name: CharSequence, values: Iterable<Any>): HttpResponse = apply {
  headers().add(name, values)
}

/** Sets the [value] of the header with the given [name] for this request, overwriting any existing values. */
public fun HttpResponse.setHeader(name: CharSequence, value: Any): HttpResponse = apply { headers().set(name, value) }

/** Sets the [values] of the header with the given [name] for this request, overwriting any existing values. */
public fun HttpResponse.setHeaders(name: CharSequence, values: Iterable<Any>): HttpResponse = apply {
  headers().set(name, values)
}

/** Remove a header with the given [name] from this request. */
public fun HttpResponse.removeHeader(name: CharSequence): HttpResponse = apply { headers().remove(name) }

/** Push raw [bytes] wrapped as a [ByteBuf][io.netty.buffer.ByteBuf] to the response body. */
public fun WritableContentStream.Writer.write(bytes: ByteArray) {
  write(Unpooled.wrappedBuffer(bytes))
}

/** Encode the given string [content] and push it to the response body. */
public fun WritableContentStream.Writer.write(content: String, charset: Charset = Charsets.UTF_8) {
  write(Unpooled.wrappedBuffer(content.toByteArray(charset)))
}

/**
 * Attach a content producer to this response body, calling [onPull] whenever new data is requested. The [onClose]
 * function is called when the producer is detached from the body due to end of data or closing.
 */
public inline fun WritableContentStream.source(
  crossinline onAttached: (WritableContentStream.Writer) -> Unit = {},
  crossinline onClose: (Throwable?) -> Unit = {},
  crossinline onPull: (WritableContentStream.Writer) -> Unit
): Unit = source(
  object : ContentStreamSource {
    @Volatile private var handle: WritableContentStream.Writer? = null

    override fun onPull() = onPull(handle ?: error("Pulled before attached"))
    override fun onAttached(writer: WritableContentStream.Writer) {
      handle = writer
      onAttached(writer)
    }

    override fun onClose(failure: Throwable?) {
      handle = null
      onClose(failure)
    }
  },
)

/**
 * Attach a content consumer to this request body, calling [onRead] whenever new data is received. The [onClose]
 * function is called when the producer is detached from the body due to end of data or closing.
 */
public inline fun ReadableContentStream.consume(
  crossinline onAttached: (ReadableContentStream.Reader) -> Unit = { it.pull() },
  crossinline onClose: (Throwable?) -> Unit = {},
  crossinline onRead: (ByteBuf, ReadableContentStream.Reader) -> Unit
): Unit = consume(
  object : ContentStreamConsumer {
    @Volatile private var handle: ReadableContentStream.Reader? = null

    override fun onRead(content: ByteBuf) = onRead(content, handle ?: error("Read before attached"))
    override fun onAttached(reader: ReadableContentStream.Reader) {
      handle = reader
      onAttached(reader)
    }

    override fun onClose(failure: Throwable?) {
      handle = null
      onClose(failure)
    }
  },
)
