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
package elide.runtime.gvm.internals.vfs

import io.micronaut.test.support.TestPropertyProvider
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.vfs.GuestVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Test injection of a file-system implementation when `vfs.mode=GUEST`. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestCase internal class EmbeddedVFSConfigTest: TestPropertyProvider {
  override fun getProperties(): MutableMap<String, String> = mutableMapOf(
    "elide.gvm.vfs.enabled" to "true",
    "elide.gvm.vfs.mode" to "GUEST",
  )

  @Inject lateinit var vfs: GuestVFS

  @Test fun testInjectable() {
    assertNotNull(vfs, "should be able to inject a VFS implementation when `mode=GUEST`")
  }

  @Test fun testExpectedImpl() {
    assertTrue(
      vfs is EmbeddedGuestVFSImpl,
      "expected embedded VFS implementation when `mode=GUEST`, instead got '${vfs::class.java.simpleName}'",
    )
  }
}
