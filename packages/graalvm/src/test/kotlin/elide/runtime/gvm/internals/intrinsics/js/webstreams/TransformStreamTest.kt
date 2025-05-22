package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.Value
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.runtime.intrinsics.js.stream.TransformStreamDefaultController
import elide.runtime.intrinsics.js.stream.TransformStreamTransformer
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@OptIn(DelicateElideApi::class)
@TestCase internal class TransformStreamTest : AbstractJsIntrinsicTest<TransformStreamIntrinsic>() {
  override fun provide(): TransformStreamIntrinsic {
    return TransformStreamIntrinsic()
  }

  override fun testInjectable() {
    // noop
  }

  private fun transformer(
    start: (controller: TransformStreamDefaultController) -> JsPromise<Unit> = { JsPromise.resolved(Unit) },
    flush: (controller: TransformStreamDefaultController) -> JsPromise<Unit> = { JsPromise.resolved(Unit) },
    transform: (chunk: Value, controller: TransformStreamDefaultController) -> JsPromise<Unit>,
  ): TransformStreamTransformer = object : TransformStreamTransformer {
    override fun start(controller: TransformStreamDefaultController) = start(controller)
    override fun flush(controller: TransformStreamDefaultController): JsPromise<Unit> = flush(controller)

    override fun transform(chunk: Value, controller: TransformStreamDefaultController): JsPromise<Unit> {
      return transform(chunk, controller)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test fun `should handle manual feed`() = runTest {
    val transformer = transformer { chunk, controller ->
      controller.enqueue(Value.asValue(chunk.asInt() + 1))
      JsPromise.resolved(Unit)
    }

    val stream = TransformDefaultStream(transformer)
    val readable = stream.readable
    val writable = stream.writable

    val writePromise = writable.getWriter().write(Value.asValue(41)).asDeferred()
    val readPromise = (readable.getReader() as ReadableStreamDefaultReader).read().asDeferred()

    awaitAll(writePromise, readPromise)

    assertEquals(
      expected = 42,
      actual = (readPromise.getCompleted().value as Value).asInt(),
    )
  }
}
