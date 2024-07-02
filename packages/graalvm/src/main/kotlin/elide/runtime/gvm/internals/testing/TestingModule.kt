/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@file:Suppress("DataClassPrivateConstructor")

package elide.runtime.gvm.internals.testing

import org.graalvm.polyglot.Value
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.testing.TestingAPI
import elide.runtime.intrinsics.testing.TestingAPI.TestGraphNode.*
import elide.vm.annotations.Polyglot

// Internal symbol where test bindings are installed.
private const val TESTING_MODULE_SYMBOL = "elide_testing"

// Installs the Elide test runner and API bindings.
@Intrinsic @Factory internal class ElideTestingModule : AbstractNodeBuiltinModule() {
  @Singleton fun provide(): TestingAPI = TestingImpl.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[TESTING_MODULE_SYMBOL.asJsSymbol()] = provide()
  }
}

// Implements Elide's guest-exposed testing APIs.
internal class TestingImpl : TestingAPI {
  companion object {
    fun obtain(): TestingAPI = TestingImpl()
  }

  @Polyglot override fun suite(label: Value?, block: Value): Suite {
    TODO("Not yet implemented: `TestingAPI.suite`")
  }

  @Polyglot override fun test(label: Value?, block: Value): Test {
    TODO("Not yet implemented: `TestingAPI.test`")
  }

  @Polyglot override fun expect(value: Value?): Assertion {
    TODO("Not yet implemented: `TestingAPI.expect`")
  }
}
