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
package elide.runtime.node

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.Test
import kotlin.test.assertContains
import elide.runtime.intrinsics.GuestIntrinsic

internal abstract class GenericJsModuleTest<T> : AbstractJsModuleTest<T>() where T: GuestIntrinsic {
  open fun requiredMembers(): Sequence<String> = emptySequence()
  open fun expectCompliance(): Boolean = true

  @Test fun `should be able to require() builtin module`() {
    require()
  }

  @Test fun `should be able to import builtin module`() {
    import()
  }

  @Test fun `should be able to load module from guest context`() {
    load()
  }

  @TestFactory open fun `module api - require(mod) should specify expected members`(): Stream<DynamicTest> {
    return requiredMembers().map { member ->
      DynamicTest.dynamicTest("$moduleName.$member") {
        val keys = require().memberKeys
        if (!expectCompliance() && !keys.contains(member)) {
          Assumptions.abort<Unit>("not yet compliant for member '$member' (module: '$moduleName')")
        } else {
          assertContains(keys, member, "member '$member' should be present on module '$moduleName'")
        }
      }
    }.asStream()
  }
}
