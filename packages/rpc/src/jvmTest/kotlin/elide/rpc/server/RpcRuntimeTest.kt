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

package elide.rpc.server

import io.grpc.ServerServiceDefinition
import io.grpc.health.v1.HealthGrpc
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows
import jakarta.inject.Inject
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Tests for the JVM-side [RpcRuntime]. */
@MicronautTest
class RpcRuntimeTest {
  @Inject internal lateinit var runtime: RpcRuntime

  @Test fun testInjectable() {
    assertNotNull(
      runtime,
      "should be able to initialize and inject RPC runtime"
    )
  }

  @Test fun testReinitializeRuntimeFail() {
    assertThrows<IllegalStateException> {
      runtime.registerServices(listOf(
        ServerServiceDefinition.builder(
          HealthGrpc.getServiceDescriptor()
        ).build()
      ))
    }
  }
}
