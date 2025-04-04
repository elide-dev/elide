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
@file:Suppress("JSCheckFunctionSignatures", "JSUnresolvedFunction")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.js.polyfills

import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests that the `Event` polyfill is available globally. */
@TestCase internal class JsEventTest : AbstractJsTest() {
  // `EventTarget` type should be present globally.
  @Test fun testEventTargetPresent() = executeGuest {
    // language=javascript
    """
      test(EventTarget).isNotNull();
    """
  }.doesNotFail()
}
