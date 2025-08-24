/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.runtime.node.stream.NodeStreamConsumersModule
import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestCase internal class NodeStreamConsumersBehaviorTest : elide.runtime.node.NodeModuleConformanceTest<NodeStreamConsumersModule>() {
  override val moduleName: String get() = "stream/consumers"
  override fun provide(): NodeStreamConsumersModule = NodeStreamConsumersModule()
  override fun expectCompliance(): Boolean = false

  @Test fun `text() - multi-chunk ReadableStream`() {
    val js = """
      const { ReadableStream } = globalThis;
      const chunks = [new Uint8Array([0x68,0x65,0x6c]), new Uint8Array([0x6c,0x6f])];
      let i = 0;
      const rs = new ReadableStream({
        pull(ctrl) {
          if (i < chunks.length) ctrl.enqueue(chunks[i++]); else ctrl.close();
        }
      });
      require('node:stream/consumers').text(rs);
    """.trimIndent()
    val result = polyglotContext.javascript(js)
    assertNotNull(result)
  }

  @Test fun `text() - AsyncIterable`() {
    val js = """
      async function* gen() {
        yield new Uint8Array([0x61]);
        yield new Uint8Array([0x62,0x63]);
      }
      require('node:stream/consumers').text(gen());
    """.trimIndent()
    val result = polyglotContext.javascript(js)
    assertNotNull(result)
  }
}

