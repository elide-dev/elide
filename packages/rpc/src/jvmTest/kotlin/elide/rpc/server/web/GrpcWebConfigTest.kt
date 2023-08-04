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

package elide.rpc.server.web

import java.time.Duration
import kotlin.test.*

/** Tests for server-side config via [GrpcWebConfig]. */
class GrpcWebConfigTest {
  @Test fun testGrpcWebConfigDefaults() {
    val default = GrpcWebConfig.DEFAULTS
    assertFalse(
      default.isEnabled,
      "gRPC-web integration should not be enabled unless explicitly enabled"
    )
    assertEquals(
      default.endpoint,
      "/_/rpc",
      "default endpoint should be expected value"
    )
    assertNotNull(
      default.timeout,
      "default timeout value should be non-null"
    )
    assertTrue(
      default.timeout.seconds > 0,
      "default timeout seconds should be a non-negative non-zero number"
    )
    assertTrue(
      default.timeout.seconds < 3600,
      "default timeout seconds should be reasonable value (less than an hour)"
    )
  }

  @Test fun testGrpcWebConfigMutability() {
    val default = GrpcWebConfig.DEFAULTS
    assertFalse(
      default.isEnabled,
      "gRPC-web integration should not be enabled unless explicitly enabled"
    )
    assertEquals(
      default.endpoint,
      "/_/rpc",
      "default endpoint should be expected value"
    )
    val enabled = object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
      override val endpoint: String get() = "/_/some-other-endpoint"
      override val timeout: Duration get() = Duration.ofSeconds(1)
    }

    assertTrue(
      enabled.isEnabled,
      "gRPC-web integration enablement should be mutable"
    )
    assertEquals(
      enabled.endpoint,
      "/_/some-other-endpoint",
      "default RPC endpoint should be mutable"
    )
    assertEquals(
      1,
      enabled.timeout.seconds,
      "should be able to override default request timeout"
    )
  }
}
