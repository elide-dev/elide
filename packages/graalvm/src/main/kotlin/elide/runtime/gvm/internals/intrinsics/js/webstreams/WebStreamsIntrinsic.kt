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
package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.TypeLiteral
import org.graalvm.polyglot.Value
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.util.concurrent.Future
import elide.core.api.Symbolic.Unresolved
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.TransformStream
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.stream.*
import elide.runtime.intrinsics.js.stream.StreamSourceType.BYTES
import elide.runtime.intrinsics.js.stream.StreamSourceType.DEFAULT
import elide.vm.annotations.Polyglot

private const val READABLE_STREAM_SYMBOL = "ReadableStream"
private const val WRITABLE_STREAM_SYMBOL = "WritableStream"
private const val TRANSFORM_STREAM_SYMBOL = "TransformStream"
private const val READABLE_BYTE_STREAM_CONTROLLER_SYMBOL = "ReadableByteStreamController"
private const val READABLE_STREAM_DEFAULT_CONTROLLER_SYMBOL = "ReadableStreamDefaultController"
private const val READABLE_STREAM_BYOB_READER = "ReadableStreamBYOBReader"
private const val READABLE_STREAM_BYOB_REQUEST = "ReadableStreamBYOBRequest"
private const val READABLE_STREAM_DEFAULT_READER = "ReadableStreamDefaultReader"
private const val TRANSFORM_STREAM_DEFAULT_CONTROLLER = "TransformStreamDefaultController"
private const val WRITABLE_STREAM_DEFAULT_CONTROLLER = "WritableStreamDefaultController"
private const val WRITABLE_STREAM_DEFAULT_WRITER = "WritableStreamDefaultWriter"
private const val EXPECTED_METHOD_START = "start"
private const val EXPECTED_METHOD_PULL = "pull"
private const val EXPECTED_METHOD_CANCEL = "cancel"

// Call the provided method; if it returns a value, try to cast it as a future, or return null.
private fun callAndCastReturnableFuture(value: Value, method: String, vararg args: Any?): Future<Any?>? {
  val ret = when {
    value.canInvokeMember(method) -> value.invokeMember(method, *args)
    value.hasMember(method) -> value.getMember(method).execute(*args)
    else -> null
  }
  return when {
    ret == null -> return null
    ret.hasMember("then") -> ret.`as`(object: TypeLiteral<Future<Any?>?>() {})
    else -> return null
  }
}

// Implements web-streams standard types and objects.
@Intrinsic internal class WebStreamsIntrinsic : AbstractJsIntrinsic() {
  // Implements a custom guest-provided readable stream source.
  internal class GuestReadableSource private constructor (
    private val value: Value,
    @Polyglot override val type: StreamSourceType,
  ) : ReadableStreamSource {
    // Starts the stream with the developer's `start` method, if any.
    @Polyglot override fun start(controller: ReadableStreamController): Future<Any?>? =
      callAndCastReturnableFuture(value, EXPECTED_METHOD_START, controller)

    // Pulls from the stream with the developer's `pull` method, if any.
    @Polyglot override fun pull(controller: ReadableStreamController): Future<Any?>? =
      callAndCastReturnableFuture(value, EXPECTED_METHOD_PULL, controller)

    // Cancels the underlying stream with the developer's `cancel` method, if any.
    @Polyglot override fun cancel(reason: String?): Future<Any?>? =
      callAndCastReturnableFuture(value, EXPECTED_METHOD_CANCEL, reason)

    companion object {
      // Creates a new `GuestReadableSource` from the given `Value`; validates certain properties before use.
      @JvmStatic fun createFrom(value: Value): GuestReadableSource {
        if (!value.hasMembers()) throw JsError.typeError("`ReadableStreamSource` requires an object")
        val type = if (value.hasMember("type")) {
          val typeToken = value.getMember("type")
          if (!typeToken.isString) throw JsError.typeError("`ReadableStreamSource.type` must be a string")
          try {
            StreamSourceType.resolve(typeToken.asString())
          } catch (err: Unresolved) {
            throw JsError.typeError("Invalid `ReadableStreamSource.type` value", cause = err)
          }
        } else {
          DEFAULT
        }
        return GuestReadableSource(value, type)
      }
    }
  }

  // Implements shared behavior for `ReadableStream` instances.
  internal sealed class ReadableStreamImpl : ReadableStream {
    internal companion object Factory : ReadableStream.Factory<ReadableStreamImpl> {
      // Implements `ReadableStream(...)`.
      override fun newInstance(vararg arguments: Value?): Any {
        val srcParam = arguments[0]
        if (srcParam == null || srcParam.isNull)
          throw JsError.typeError("First parameter ('source') for `ReadableStream` is required")

        // create a source out of the user's provided type
        val src = try {
          ReadableStreamSource.from(srcParam)
        } catch (err: PolyglotException) {
          throw JsError.typeError("Invalid source provided to `ReadableStream`", cause = err)
        }
        // resolve the provided queueing strategy, if an
        val strategy = arguments.getOrNull(1)
          ?.takeIf { !it.isNull }
          ?.`as`(QueuingStrategy::class.java)
        return create(src, strategy)
      }

      @Polyglot override fun create(source: ReadableStreamSource, queueingStrategy: QueuingStrategy?): ReadableStream =
        when (source.type) {
          BYTES -> GuestReadableByteStreamImpl(source, ReadableByteStreamController.create(queueingStrategy))
          DEFAULT -> GuestReadableStreamDefaultImpl(source, ReadableStreamDefaultController.create(queueingStrategy))
        }

      override fun empty(): ReadableStreamImpl {
        TODO("Not yet implemented")
      }

      override fun wrap(input: InputStream): ReadableStreamImpl {
        TODO("Not yet implemented")
      }

      override fun wrap(reader: Reader): ReadableStreamImpl {
        TODO("Not yet implemented")
      }

      override fun wrap(bytes: ByteArray): ReadableStreamImpl {
        TODO("Not yet implemented")
      }

      override fun wrap(buffer: ByteBuffer): ReadableStreamImpl {
        TODO("Not yet implemented")
      }
    }
  }

  internal sealed class GuestReadableStreamImpl<Controller: ReadableStreamController> (
    private val source: ReadableStreamSource,
    private val controller: Controller,
  ) : ReadableStreamImpl()

  internal class GuestReadableByteStreamImpl internal constructor (
    source: ReadableStreamSource,
    controller: ReadableByteStreamController,
  ) : GuestReadableStreamImpl<ReadableByteStreamController>(source, controller)

  internal class GuestReadableStreamDefaultImpl internal constructor (
    source: ReadableStreamSource,
    controller: ReadableStreamDefaultController,
  ) : GuestReadableStreamImpl<ReadableStreamDefaultController>(source, controller)

  // Implements shared behavior for `WritableStream` instances.
  internal abstract class WritableStreamImpl : WritableStream {
    internal companion object Factory : WritableStream.Factory<WritableStreamImpl> {

    }
  }

  // Implements shared behavior for `TransformStream` instances.
  internal abstract class TransformStreamImpl : TransformStream {
    internal companion object Factory : TransformStream.Factory<TransformStreamImpl> {

    }
  }

  @OptIn(DelicateElideApi::class)
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[READABLE_STREAM_SYMBOL.asPublicJsSymbol()] = ReadableStreamImpl.Factory
    bindings[WRITABLE_STREAM_SYMBOL.asPublicJsSymbol()] = WritableStreamImpl
    bindings[TRANSFORM_STREAM_SYMBOL.asPublicJsSymbol()] = TransformStreamImpl
    bindings[READABLE_BYTE_STREAM_CONTROLLER_SYMBOL.asPublicJsSymbol()] = ReadableByteStreamController
    bindings[READABLE_STREAM_DEFAULT_CONTROLLER_SYMBOL.asPublicJsSymbol()] = ReadableStreamDefaultController
    bindings[READABLE_STREAM_BYOB_READER.asPublicJsSymbol()] = ReadableStreamBYOBReader::class.java
    bindings[READABLE_STREAM_BYOB_REQUEST.asPublicJsSymbol()] = ReadableStreamBYOBRequest::class.java
    bindings[READABLE_STREAM_DEFAULT_READER.asPublicJsSymbol()] = ReadableStreamDefaultReader::class.java
    bindings[TRANSFORM_STREAM_DEFAULT_CONTROLLER.asPublicJsSymbol()] = TransformStreamDefaultController::class.java
    bindings[WRITABLE_STREAM_DEFAULT_CONTROLLER.asPublicJsSymbol()] = WritableStreamDefaultController::class.java
    bindings[WRITABLE_STREAM_DEFAULT_WRITER.asPublicJsSymbol()] = WritableStreamDefaultWriter::class.java
  }
}
