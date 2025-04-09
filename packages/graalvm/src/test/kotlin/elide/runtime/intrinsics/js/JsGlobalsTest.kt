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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_ALL
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.Symbol
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.vfs.vfs
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

private const val ENABLE_POLYFILLS = true
private const val ENABLE_SUPPRESSIONS = true

@TestCase internal class JsGlobalsTest : AbstractJsTest() {
  private val polyfillsContent = requireNotNull(javaClass.getResource(
    "/META-INF/elide/embedded/runtime/js/polyfills.js"
  )) {
    "Failed to locate JS polyfills"
  }.readText()

  private val polyfillsSrc = Source.create("js", polyfillsContent)

  @Inject private lateinit var intrinsics: IntrinsicsManager

  // Standards-compliant (ECMA) symbols.
  // See: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects
  private val standardJsGlobals = sortedSetOf(
    "globalThis",
    "Infinity",
    "isFinite",
    "isNaN",
    "parseFloat",
    "parseInt",
    "queueMicrotask",
    "decodeURI",
    "decodeURIComponent",
    "encodeURI",
    "encodeURIComponent",
    "escape",
    "unescape",
    "navigator",
    "structuredClone",
    "Navigator",
    "Object",
    "Function",
    "Boolean",
    "Symbol",
    "Error",
    "AggregateError",
    "EvalError",
    "RangeError",
    "ReferenceError",
    "SyntaxError",
    "TypeError",
    "URIError",
    "InternalError",
    "Number",
    "BigInt",
    "Math",
    "Date",
    "Temporal",
    "String",
    "RegExp",
    "Array",
    "Int8Array",
    "Uint8Array",
    "Uint8ClampedArray",
    "Int16Array",
    "Uint16Array",
    "Int32Array",
    "Uint32Array",
    "BigInt64Array",
    "BigUint64Array",
    "Float16Array",
    "Float32Array",
    "Float64Array",
    "Set",
    "Map",
    "WeakMap",
    "WeakSet",
    "ArrayBuffer",
    "SharedArrayBuffer",
    "DataView",
    "Atomics",
    "JSON",
    "WeakRef",
    "FinalizationRegistry",
    "Iterator",
    "AsyncIterator",
    "Promise",
    "GeneratorFunction",
    "AsyncGeneratorFunction",
    "Generator",
    "AsyncGenerator",
    "AsyncFunction",
    "Reflect",
    "Proxy",
    "Intl",
    "WebAssembly",
  )

  // Expected Node-specific global symbols.
  // See: https://nodejs.org/api/globals.html
  private val nodeSpecificGlobals = sortedSetOf(
    "AbortController",
    "Blob",
    "Buffer",
    "atob",
    "BroadcastChannel",
    "CustomEvent",
    "btoa",
    "clearImmediate",
    "clearInterval",
    "clearTimeout",
    "CloseEvent",
    "CompressionStream",
    "console",
    "CountQueueingStrategy",
    "Crypto",
    "crypto",
    "CryptoKey",
    "DecompressionStream",
    "Event",
    "EventSource",
    "EventTarget",
    "fetch",
    "File",
    "FormData",
    "PerformanceEntry",
    "PerformanceMark",
    "PerformanceMeasure",
    "PerformanceObserver",
    "PerformanceObserverEntryList",
    "performance",
    "process",
    "ReadableByteStreamController",
    "ReadableStream",
    "ReadableStreamBYOBReader",
    "ReadableStreamBYOBRequest",
    "ReadableStreamDefaultController",
    "ReadableStreamDefaultReader",
    "require",
    "setImmediate",
    "setInterval",
    "setTimeout",
    "Storage",
    "SubtleCrypto",
    "DOMException",
    "TextDecoder",
    "TextDecoderStream",
    "TextEncoder",
    "TextEncoderStream",
    "TransformStream",
    "TransformStreamDefaultController",
    "URL",
    "URLSearchParams",
    "WebSocket",
    "WritableStream",
    "WritableStreamDefaultController",
    "WritableStreamDefaultWriter",
  )

  // Web streams standard types.
  private val streamGlobals = sortedSetOf(
    "ReadableStream",
    "WritableStream",
    "TransformStream",
  )

  private val fetchGlobals = listOf(
    "fetch",
    "Headers",
    "Request",
    "Response",
  )

  private val urlGlobals = listOf(
    "URL",
    "URLSearchParams",
  )

  // --------------

  // Globals which may fail to be found without failing tests (for instance, still under implementation).
  private val allowMissingGlobals = sortedSetOf(
    "setImmediate",  // not yet implemented
    "clearImmediate",  // not yet implemented
    "InternalError",  // web-standard only, not present in non-browser runtimes
    "BroadcastChannel",  // not yet implemented
    "CloseEvent",  // not yet implemented
    "CountQueueingStrategy",  // not yet implemented
    "CompressionStream",  // not yet implemented
    "DecompressionStream",  // not yet implemented
    "Crypto",  // not yet implemented
    "CryptoKey", // not yet implemented
    "Event",  // not yet implemented
    "EventSource",  // not yet implemented
    "FormData",  // not yet implemented
    "WebSocket",  // not yet implemented
    "PerformanceEntry",  // not yet implemented
    "PerformanceMark",  // not yet implemented
    "PerformanceMeasure",  // not yet implemented
    "PerformanceObserver",  // not yet implemented
    "PerformanceObserverEntryList",  // not yet implemented
    "Storage",  // not yet implemented
    "SubtleCrypto",  // not yet implemented
    "TextDecoderStream",  // not yet implemented
    "TextEncoderStream",  // not yet implemented
    "DOMException",  // not yet implemented
  )

  // Types which are expected to be provided by JS polyfills.
  private val expectedPolyfills = streamGlobals.plus(sortedSetOf(
    "ReadableByteStreamController",
    "ReadableStreamBYOBReader",
    "ReadableStreamBYOBRequest",
    "ReadableStreamDefaultController",
    "ReadableStreamDefaultReader",
    "TransformStreamDefaultController",
    "WritableStreamDefaultController",
    "WritableStreamDefaultWriter",
  ).plus(
    fetchGlobals
  )).toSortedSet()

  // Globals which are expected not to be found host-side.
  private val expectMissingHostGlobals = standardJsGlobals.plus(sortedSetOf(
    "globalThis",  // not valid in the context of host code
    "performance",  // implemented as part of GraalJs
    "Map",  // implemented as part of GraalJs
    "Set",  // implemented as part of GraalJs
    "InternalError",  // web-standard only, not present in non-browser runtimes
    "require",  // guest-only symbol
  )).plus(
    expectedPolyfills  // polyfills are guest-only
  )

  // Fully-qualified property paths which are expected to be non-null within guest JS contexts.
  private val guestPaths = sortedSetOf(
    "Function.prototype.once",
    "JSON.stringify",
    "JSON.parse",
    "Intl.Collator",
    "Intl.DateTimeFormat",
    "Intl.DisplayNames",
    "Intl.DurationFormat",
    "Intl.Locale",
    "Intl.NumberFormat",
    "Intl.PluralRules",
    "Intl.RelativeTimeFormat",
    "Intl.Segmenter",
  )

  // Globals which are expected not to be found host-side.
  private val expectMissingGuestGlobals: Set<String> = setOf(
    "InternalError",  // web-standard only, not present in non-browser runtimes
    "Float16Array",  // not yet implemented
    "AsyncFunction",  // symbolic in nature
    "AsyncGenerator",  // symbolic in nature
    "AsyncGeneratorFunction",  // symbolic in nature
    "Generator",  // symbolic in nature
    "GeneratorFunction",  // symbolic in nature
  )

  // All globals which are expected to exist in a vanilla Elide JS context.
  private val expectedGlobals = standardJsGlobals.plus(sortedSetOf(
    "AbortController",
    "AbortSignal",
    "Base64",
    "Console",
    "Elide",
    "EventTarget",
    "File",
    "ValueError",
  )).plus(
    nodeSpecificGlobals
  ).plus(
    fetchGlobals
  ).plus(
    urlGlobals
  ).plus(
    streamGlobals
  )

  @DelicateElideApi
  private fun withFreshContext(block: suspend Context.() -> Unit): Unit = PolyglotEngine {
    hostAccess = ALLOW_ALL

    vfs {
      useHost = true
    }

    configure(JavaScript) {
      npm {
        enabled = true
        modulesPath = System.getenv("PWD")
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

  @DelicateElideApi
  @Test fun `polyfills script can execute`() {
    withFreshContext {
      eval(polyfillsSrc)
    }
  }

  @DelicateElideApi
  @TestFactory fun `polyfills script provides expected globals`() = sequence<DynamicTest> {
    expectedPolyfills.forEach { polyfilledName ->
      yield(
        DynamicTest.dynamicTest(polyfilledName) {
          withFreshContext {
            eval(polyfillsSrc).let { value ->
              val bindings = value.context.getBindings("js")
              val obj = assertNotNull(
                bindings.getMember(polyfilledName),
                "global symbol '$polyfilledName' is missing from polyfilled bindings",
              )
              assertFalse(obj.isNull, "global symbol '$polyfilledName' cannot be null")
            }
          }
        }
      )
    }
  }.toList()

  @DelicateElideApi
  @TestFactory fun `global bindings`(): List<DynamicTest> = sequence<DynamicTest> {
    val symbols = intrinsics.resolver().resolve(GraalVMGuest.JAVASCRIPT, internals = true).toList()
    val mapped = mutableMapOf<Symbol, Any>()
    val bindings = MutableIntrinsicBindings.Factory.wrap(mapped)
    symbols.map { it as AbstractJsIntrinsic }.map {
      it.install(bindings)
    }
    expectedGlobals.forEach { globalName ->
      if (globalName !in expectMissingHostGlobals) yield(DynamicTest.dynamicTest("$globalName (host)") {
        if (ENABLE_SUPPRESSIONS && globalName in allowMissingGlobals) {
          Assumptions.abort<Unit>("Global $globalName (host) is known-missing")
        }
        assertNotNull(
          bindings[globalName.asPublicJsSymbol()],
          "global symbol '$globalName' is missing from bindings",
        )
      })
      if (globalName !in expectMissingGuestGlobals) yield(DynamicTest.dynamicTest("$globalName (guest)") {
        if (ENABLE_SUPPRESSIONS && globalName in allowMissingGlobals) {
          Assumptions.abort<Unit>("Global $globalName (guest) is known-missing")
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
      })
    }
  }.toList()

  @DelicateElideApi
  @TestFactory fun `guest-only globals`(): List<DynamicTest> = sequence<DynamicTest> {
    guestPaths.forEach {
      yield(DynamicTest.dynamicTest(it) {
        executeGuest {
          // language=JavaScript
          """
             $it;
          """
        }
      })
    }
  }.toList()
}
