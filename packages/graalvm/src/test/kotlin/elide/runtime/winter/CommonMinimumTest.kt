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
package elide.runtime.winter

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.use
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_ALL
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.Symbol
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.vfs.vfs
import elide.runtime.plugins.wasm.Wasm
import elide.testing.annotations.TestCase

private const val ENABLE_POLYFILLS = true
private const val ENABLE_SUPPRESSIONS = true

@TestCase internal class CommonMinimumTest : AbstractJsTest() {
  private val polyfillsContent = requireNotNull(javaClass.getResource(
    "/META-INF/elide/embedded/runtime/js/polyfills.js"
  )) {
    "Failed to locate JS polyfills"
  }.readText()

  private val polyfillsSrc = Source.create("js", polyfillsContent)

  @Inject private lateinit var intrinsics: IntrinsicsManager

  // Symbols which are expected to be missing.
  private val expectMissingGlobals = sortedSetOf<String>()

  // Symbols which can be missing.
  private val allowMissingGlobals = sortedSetOf<String>(
    "CompressionStream",
    "Crypto",
    "CryptoKey",
    "DOMException",
    "DecompressionStream",
    "Event",
    "FormData",
    "SubtleCrypto",
    "TextDecoderStream",
    "TextEncoderStream",
    "URLPattern",
    "navigator.userAgent",
    "structuredClone",
  )

  // Minimum Common API §3.1: Interfaces.
  // See: https://min-common-api.proposal.wintertc.org/#api-index
  private val minimumCommonInterfaces = sortedSetOf(
    "AbortController",
    "AbortSignal",
    "Blob",
    "ByteLengthQueuingStrategy",
    "CompressionStream",
    "CountQueuingStrategy",
    "Crypto",
    "CryptoKey",
    "DecompressionStream",
    "DOMException",
    "Event",
    "EventTarget",
    "File",
    "FormData",
    "Headers",
    "ReadableByteStreamController",
    "ReadableStream",
    "ReadableStreamBYOBReader",
    "ReadableStreamBYOBRequest",
    "ReadableStreamDefaultController",
    "ReadableStreamDefaultReader",
    "Request",
    "Response",
    "SubtleCrypto",
    "TextDecoder",
    "TextDecoderStream",
    "TextEncoder",
    "TextEncoderStream",
    "TransformStream",
    "TransformStreamDefaultController",
    "URL",
    "URLPattern",
    "URLSearchParams",
    "WebAssembly.Global",
    "WebAssembly.Instance",
    "WebAssembly.Memory",
    "WebAssembly.Module",
    "WebAssembly.Table",
    "WritableStream",
    "WritableStreamDefaultController",
    "WritableStreamDefaultWriter"
  )

  // Minimum Common API §3.2: Global methods / properties.
  // See: https://min-common-api.proposal.wintertc.org/#api-index
  private val commonMinimumMethodsAndProperties = sortedSetOf(
    "globalThis",
    "atob",
    "btoa",
    "clearInterval",
    "clearTimeout",
    "console",
    "crypto",
    "fetch",
    "navigator.userAgent",
    "performance.now",
    "performance.timeOrigin",
    "queueMicrotask",
    "setInterval",
    "setTimeout",
    "structuredClone",
    "WebAssembly.compile",
    "WebAssembly.compileStreaming",
    "WebAssembly.instantiate",
    "WebAssembly.instantiateStreaming",
    "WebAssembly.validate",
  )

  @DelicateElideApi
  private fun withFreshContext(block: suspend Context.() -> Unit): Unit = PolyglotEngine {
    hostAccess = ALLOW_ALL

    vfs {
      useHost = true
    }

    install(Wasm)
    install(JavaScript) {
      npm {
        enabled = true
        modulesPath = System.getenv("PWD")
        wasm = true
      }
    }
  }.let { engine ->
    engine.acquire {
      build().use {
        runTest {
          block.invoke(it)
        }
      }
    }
    Unit
  }

  @DelicateElideApi
  private fun executeGuest(
    stdEngine: Boolean = true,
    block: () -> String,
  ) {
    if (stdEngine) this.executeGuest(bind = true) {
      StringBuilder().apply {
        if (ENABLE_POLYFILLS) appendLine(polyfillsContent)
        append(block())
      }.toString()
    }.let {
      it.doesNotFail()
      it.returnValue().let { symbolValue ->
        assertNotNull(symbolValue, "should get value from guest execution")
        assertFalse(symbolValue.isNull, "guest value should not be `null`")
        assertFalse(symbolValue.isBoolean && !symbolValue.asBoolean(), "value should not be `false`")
      }
    } else withFreshContext {
      if (ENABLE_POLYFILLS) eval(polyfillsSrc)
      eval(Source.create("js", block.invoke())).let { value ->
        assertNotNull(value, "should get value from guest execution")
        assertFalse(value.isNull, "value should not be `null`")
        assertFalse(value.isBoolean && !value.asBoolean(), "value should not be `false`")
      }
    }
  }

  @OptIn(DelicateElideApi::class)
  suspend fun SequenceScope<DynamicTest>.testFactory(globalName: String) {
    if (globalName !in expectMissingGlobals) yield(
      DynamicTest.dynamicTest(globalName) {
        if (ENABLE_SUPPRESSIONS && globalName in allowMissingGlobals) {
          Assumptions.abort<Unit>("Common Minimum API '$globalName' is known-missing")
        }
        executeGuest {
          // language=JavaScript
          """
             function pluck() {
               return $globalName;
             }
             pluck();
          """
        }
      }
    )
  }

  @DelicateElideApi
  @TestFactory fun `minimum common api - interfaces`(): List<DynamicTest> = sequence<DynamicTest> {
    val symbols = intrinsics.resolver().resolve(GraalVMGuest.JAVASCRIPT, internals = true).toList()
    val mapped = mutableMapOf<Symbol, Any>()
    val bindings = MutableIntrinsicBindings.Factory.wrap(mapped)
    symbols.map { it as AbstractJsIntrinsic }.map {
      it.install(bindings)
    }
    minimumCommonInterfaces.forEach { globalName ->
      testFactory(globalName)
    }
  }.toList()

  @DelicateElideApi
  @TestFactory fun `minimum common api - globals`(): List<DynamicTest> = sequence<DynamicTest> {
    val symbols = intrinsics.resolver().resolve(GraalVMGuest.JAVASCRIPT, internals = true).toList()
    val mapped = mutableMapOf<Symbol, Any>()
    val bindings = MutableIntrinsicBindings.Factory.wrap(mapped)
    symbols.map { it as AbstractJsIntrinsic }.map {
      it.install(bindings)
    }
    commonMinimumMethodsAndProperties.forEach { globalName ->
      testFactory(globalName)
    }
  }.toList()
}
