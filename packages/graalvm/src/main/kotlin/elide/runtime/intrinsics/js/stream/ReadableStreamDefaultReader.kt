package elide.runtime.intrinsics.js.stream

import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.vm.annotations.Polyglot

/**
 * A reader used by streams in default mode, used to read arbitrary chunks of data. Default readers do not support
 * BYOB operations.
 */
public interface ReadableStreamDefaultReader : ReadableStreamReader {
    /** Read a chunk from the stream, returning a promise that is fulfilled with the result. */
    @Polyglot public fun read(): JsPromise<ReadResult>
}
