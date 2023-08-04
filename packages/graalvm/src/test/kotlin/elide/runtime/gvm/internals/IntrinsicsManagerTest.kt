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

@file:Suppress("MnInjectionPoints")

package elide.runtime.gvm.internals

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import jakarta.inject.Inject
import elide.runtime.intrinsics.GuestIntrinsic

/** Tests for the [IntrinsicsManager]. */
@MicronautTest class IntrinsicsManagerTest {
  @Inject private lateinit var manager: IntrinsicsManager
  @Inject private lateinit var intrinsics: Collection<GuestIntrinsic>

  @Test fun testInjectable() {
    assertNotNull(manager, "should be able to inject intrinsics manager")
    assertTrue(intrinsics.isNotEmpty(), "should be able to collect intrinsics via injection")
  }

  @Test fun testResolveIntrinsics() {
    val resolved = manager.resolver().resolve(GraalVMGuest.JAVASCRIPT)
    assertNotNull(resolved, "should not get `null` from intrinsics resolver")
    assertTrue(resolved.isNotEmpty(), "resolved set of intrinsics should not be empty")
  }
}
