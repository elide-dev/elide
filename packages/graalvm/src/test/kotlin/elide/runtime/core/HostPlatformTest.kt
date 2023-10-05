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

package elide.runtime.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(DelicateElideApi::class)
internal class HostPlatformTest {
  @Test fun testDetectOS() {
    for ((source, result) in osCases) assertEquals(
      expected = result,
      actual = HostPlatform.parseOperatingSystem(source),
      message = "should detect OS family",
    )
  }

  @Test fun testDetectArch() {
    for ((source, result) in archCases) assertEquals(
      expected = result,
      actual = HostPlatform.parseArchitecture(source),
      message = "should detect architecture",
    )
  }

  @Test fun testDetectPlatform() {
    for ((osSource, osResult) in osCases) for ((archSource, archResult) in archCases) {
      assertEquals(
        expected = HostPlatform(osResult, archResult),
        actual = HostPlatform.parsePlatform("${osSource}_$archSource"),
        message = "should detect platform",
      )
    }
  }

  private companion object {
    val osCases = arrayOf(
      "linux" to HostPlatform.OperatingSystem.LINUX,
      "darwin" to HostPlatform.OperatingSystem.DARWIN,
      "macos" to HostPlatform.OperatingSystem.DARWIN,
      "windows" to HostPlatform.OperatingSystem.WINDOWS,
    )

    val archCases = arrayOf(
      "amd64" to HostPlatform.Architecture.AMD64,
      "x86_64" to HostPlatform.Architecture.AMD64,
      "arm64" to HostPlatform.Architecture.ARM64,
      "aarch64" to HostPlatform.Architecture.ARM64,
    )
  }
}
