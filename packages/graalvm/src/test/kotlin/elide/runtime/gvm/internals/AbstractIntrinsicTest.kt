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

package elide.runtime.gvm.internals

import org.junit.jupiter.api.Assertions.*
import elide.runtime.gvm.internals.AbstractDualTest.CodeGenerator
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.Symbol
import elide.testing.annotations.Test

/** Abstract base for intrinsic-bound tests. */
abstract class AbstractIntrinsicTest<T : GuestIntrinsic, Code: CodeGenerator> : AbstractDualTest<Code>() {
  /** @return Intrinsic implementation under test. */
  protected abstract fun provide(): T

  /** Test injection of an intrinsic implementation. */
  @Test abstract fun testInjectable()

  /** Test installation of the intrinsic. */
  @Test fun testInstall() {
    val target = HashMap<Symbol, Any>()
    val subject = provide()
    assertNotNull(subject, "should not get `null` subject from `provide` for intrinsic under test")
    assertDoesNotThrow {
      subject.install(MutableIntrinsicBindings.Factory.wrap(target))
    }
    assertTrue(target.isNotEmpty(), "should have at least one intrinsic binding installed")
  }
}
