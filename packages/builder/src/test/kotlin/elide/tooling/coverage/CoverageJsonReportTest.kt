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
package elide.tooling.coverage

import elide.testing.annotations.Test
import kotlin.test.*

class CoverageJsonReportTest {
  @Test fun testParseReport() {
    val data = CoverageJsonReportTest::class.java.getResourceAsStream("/coverage/coverage.json")
    assertNotNull(data)
    val report = CoverageJsonReport.parse(data.buffered())
    assertNotNull(report)
    assertTrue(report.entries.isNotEmpty())
  }

  @Test fun testParseReportStable() {
    val data = CoverageJsonReportTest::class.java.getResourceAsStream("/coverage/coverage.json")
    assertNotNull(data)
    val report = CoverageJsonReport.parse(data.buffered())
    assertNotNull(report)
    assertTrue(report.entries.isNotEmpty())
    val serialized = report.toJson()
    val other = report.toJson()
    assertNotNull(serialized)
    assertEquals(serialized, other)
    val decodedAgain = CoverageJsonReport.parse(serialized)
    assertNotNull(decodedAgain)
    assertEquals(report.entries.size, decodedAgain.entries.size)
  }
}
