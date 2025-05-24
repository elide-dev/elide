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

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.TransformStream
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.runtime.intrinsics.js.stream.TransformStreamDefaultController
import elide.runtime.intrinsics.js.stream.TransformStreamTransformer
import elide.testing.annotations.TestCase
import elide.runtime.plugins.js.JavaScript as JS

@OptIn(DelicateElideApi::class)
@TestCase internal class TransformStreamTest : AbstractJsIntrinsicTest<TransformStreamIntrinsic>() {
  @Inject lateinit var intrinsic: TransformStreamIntrinsic

  override fun provide(): TransformStreamIntrinsic = intrinsic
  override fun testInjectable() {
    assertNotNull(intrinsic)
  }

  private fun mappingTransformer(map: (Value) -> Value): TransformStreamTransformer {
    return object : TransformStreamTransformer {
      override fun transform(chunk: Value, controller: TransformStreamDefaultController): JsPromise<Unit> {
        controller.enqueue(map(chunk))
        return JsPromise.resolved(Unit)
      }
    }
  }

  @Test fun `should allow creating transform streams`() = runTest {
    // host-side construction
    assertDoesNotThrow("expected stream to be created from host call") {
      TransformStream.create(TransformStreamTransformer.Identity)
    }

    // guest-side construction
    executeGuest {
      bindings(JS).putMember("TestTransformer", TransformStreamTransformer.Identity)
      "new TransformStream(TestTransformer)"
    }.thenAssert {
      it.doesNotFail()

      val stream = assertNotNull(it.returnValue())
      assertDoesNotThrow("expected a transform stream instance") { stream.asProxyObject<TransformStream>() }
    }
  }

  @Test fun `should transform values with host transformer`() = runTest {
    val transform = mappingTransformer { Value.asValue(it.asInt() + 1) }
    val stream = TransformStream.create(transform)

    val reader = stream.readable.getReader() as ReadableStreamDefaultReader
    val writer = stream.writable.getWriter()

    writer.write(Value.asValue(41))
    val transformed = reader.read().asDeferred().await()

    assertEquals(
      expected = 42,
      actual = transformed.value?.asInt(),
    )
  }

  @Test fun `should transform values with guest transformer`() = runTest {
    val result = CompletableDeferred<Value?>()

    executeGuest {
      """
      const stream = new TransformStream({ transform: async (chunk, controller) => controller.enqueue(chunk + 1) });
      const writer = stream.writable.getWriter();
      writer.write(41);
      stream.readable
      """.trimIndent()
    }.thenAssert { test ->
      val stream = assertNotNull(test.returnValue()).asProxyObject<ReadableStream>()
      val reader = stream.getReader() as ReadableStreamDefaultReader

      reader.read().then(
        onFulfilled = { result.complete(it.value) },
        onCatch = { result.completeExceptionally(TypeError.create(it.toString())) },
      )
    }

    assertEquals(
      expected = 42,
      actual = result.await()?.asInt(),
    )
  }
}
