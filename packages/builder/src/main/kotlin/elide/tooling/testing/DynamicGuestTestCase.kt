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
package elide.tooling.testing

import elide.runtime.core.PolyglotContext
import elide.runtime.intrinsics.testing.TestEntrypoint

/**
 * A test case registered by guest-side code, e.g. by using intrinsics. The [entrypointProvider] function can be used
 * to obtain an executable value encapsulating the guest test code.
 */
public class DynamicGuestTestCase(
  override val id: TestNodeKey,
  override val parent: TestNodeKey?,
  override val displayName: String,
  public val entrypointProvider: (PolyglotContext) -> TestEntrypoint,
) : TestCase {
  override val type: TestTypeKey<DynamicGuestTestCase> get() = DynamicGuestTestCase

  public companion object : TestTypeKey<DynamicGuestTestCase>
}
