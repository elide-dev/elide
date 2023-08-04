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

package elide.server.controller.builtin

import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Contract tests for [NotFoundController]. */
@MicronautTest class NotFoundControllerTest : BuiltinControllerTest<NotFoundController>() {
  @Inject lateinit var controller: NotFoundController

  @Test fun testInjectable() {
    assertNotNull(controller)
  }

  override fun controller(): NotFoundController {
    return controller
  }

  override fun getRequestTemplate(method: HttpMethod): MutableHttpRequest<Any> {
    return HttpRequest.create(method, "/error/notfound")
  }
}
