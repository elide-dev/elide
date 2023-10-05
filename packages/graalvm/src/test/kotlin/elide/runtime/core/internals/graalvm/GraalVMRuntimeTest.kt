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

package elide.runtime.core.internals.graalvm

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi

@OptIn(DelicateElideApi::class)
class GraalVMRuntimeTest {
  @Test fun testResolveVersion() {
    val cases = arrayOf(
      "20.0.2+9-jvmci-23.0-b14" to GraalVMRuntime.GVM_23,
      "21+35-jvmci-23.1-b15" to GraalVMRuntime.GVM_23_1,
    )
    
    for ((source, version) in cases) assertEquals(
      expected = version,
      actual = GraalVMRuntime.resolveVersion(source),
      message = "should resolve version $version from '$source'",
    )
  }
}
