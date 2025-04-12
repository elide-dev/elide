package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.junit.jupiter.api.Assertions.assertFalse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultController
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.runtime.intrinsics.js.stream.ReadableStreamSource
import elide.testing.annotations.Test

class ReadableStreamTest {
  private inline fun pushSource(crossinline onStart: (ReadableStreamDefaultController) -> Unit): ReadableStreamSource {
    return object : ReadableStreamSource {
      override fun start(controller: ReadableStreamController) = onStart(controller as ReadableStreamDefaultController)
    }
  }

  private inline fun <R> ReadableStream.useReader(block: (ReadableStreamDefaultReader) -> R): R {
    val reader = getReader() as ReadableStreamDefaultReader
    return runCatching { block(reader) }
      .also { reader.releaseLock() }
      .getOrThrow()
  }

  private fun ReadableStreamDefaultReader.readAsync(): Deferred<ReadableStream.ReadResult> = read().asDeferred()
  private suspend fun ReadableStreamDefaultReader.awaitRead(): ReadableStream.ReadResult = readAsync().await()

  @Test fun `should save chunks in queue when no pending reads`() = runTest {
    val source = pushSource { it.enqueue(1) }
    val stream = ReadableStream.create(source)

    val read = stream.useReader { reader -> reader.awaitRead() }
    assertEquals(1, read.chunk, "expected chunk to have enqueued value")
    assertFalse(read.done, "expected chunk not to be the last")
  }

  @Test fun `should add reads to queue when no available chunks`() = runTest {
    
  }

  @Test fun `should resolve pending reads with new chunk without using queue`() = runTest {

  }

  @Test fun `should pull from source when source queue is empty`() = runTest {

  }

  @Test fun `should not pull from source when backpressure is applied`() = runTest {

  }

  @Test fun `should wait for queued chunks before closing`() = runTest {

  }

  @Test fun `should discard queued chunks and reads on error`() = runTest {

  }

  @Test fun `should handle data from host push source`() = runTest {
    // simple host source (guest support TBD)
    val source = pushSource { controller ->
      // push-only, no backpressure control (for simplicity), last chunk should be marked
      // as final and close the stream when read
      controller.enqueue(1)
      controller.enqueue(2)
      controller.enqueue(3)
      controller.close()
    }

    // cast is needed on the host to access the APIs (for now, will be easier later)
    val stream = ReadableStream.create(source)
    val reader = stream.getReader() as ReadableStreamDefaultReader

    // read from the stream sequentially until a chunk marked as 'done' is found
    val chunks = flow {
      do {
        val chunk = reader.read().asDeferred().await(); emit(chunk)
      } while (!chunk.done)
    }.toList()

    // inspect results
    assertEquals(3, chunks.size, "expected 3 values")
    repeat(chunks.size) { assertEquals(it + 1, chunks[it].chunk) }
  }
}
