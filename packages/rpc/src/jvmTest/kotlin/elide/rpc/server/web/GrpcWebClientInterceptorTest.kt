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

import io.grpc.Status
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Basic tests for state held by [GrpcWebClientInterceptor]. */
class GrpcWebClientInterceptorTest {
  @Test fun testCreateNewInterceptor() {
    assertNotNull(
      GrpcWebClientInterceptor(CountDownLatch(0)),
      "should be able to construct a client interceptor from scratch"
    )
  }

  @Test fun testInitialInterceptorState() {
    val interceptor = GrpcWebClientInterceptor(CountDownLatch(0))
    assertEquals(
      Status.INTERNAL,
      interceptor.terminalStatus.get(),
      "default response status should be `INTERNAL`"
    )
    assertNotNull(
      interceptor.headers,
      "default set of intercepted response headers should not be `null`"
    )
    assertNotNull(
      interceptor.trailers,
      "default set of intercepted response trailers should not be `null`"
    )
  }

  @Test fun testInitialInterceptorStateMutability() {
    val interceptor = GrpcWebClientInterceptor(CountDownLatch(0))
    assertEquals(
      Status.INTERNAL,
      interceptor.terminalStatus.get(),
      "default response status should be `INTERNAL`"
    )
    assertNotNull(
      interceptor.headers,
      "default set of intercepted response headers should not be `null`"
    )
    assertNotNull(
      interceptor.trailers,
      "default set of intercepted response trailers should not be `null`"
    )
    interceptor.terminalStatus.set(Status.OK)
    assertEquals(
      Status.OK,
      interceptor.terminalStatus.get(),
      "status on interceptor should be mutable via atomic reference"
    )
  }
}
