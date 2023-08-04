/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.js

import io.micronaut.context.BeanContext
import org.junit.jupiter.api.Assertions.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the V3 JS runtime implementation, on top of GraalVM. */
@TestCase class JsRuntimeTest {
  // JS runtime singleton.
  @Inject internal lateinit var runtime: JsRuntime

  // Micronaut bean context.
  @Inject internal lateinit var beanContext: BeanContext

  @Test fun testInjectable() {
    assertNotNull(runtime, "should be able to inject JS runtime instance")
  }

  @Test fun testSingleton() {
    assertNotNull(runtime, "should be able to inject JS runtime factory instance")
    assertNotNull(beanContext, "should be able to inject bean context")
    assertNotNull(runtime, "should be able to create JS runtime instance")
  }
}
