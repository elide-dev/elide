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

package elide.http

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.http.api.*
import elide.http.api.HttpHeaders
import elide.http.api.HttpMessage
import elide.http.api.HttpPayload
import elide.http.api.MutableHttpHeaders
import elide.http.api.MutableHttpMessage
import elide.http.api.MutableHttpPayload

class HttpMessageTest {
  @Test fun testImmutableInterface() {
    assertFalse(object: HttpMessage {
      override val type: HttpMessageType
        get() = error("not implemented")
      override val version: HttpVersion
        get() = error("not implemented")
      override val headers: HttpHeaders
        get() = error("not implemented")
      override val body: HttpPayload
        get() = error("not implemented")
    }.mutable)
  }

  @Test fun testMutableInterface() {
    assertTrue(object: MutableHttpMessage {
      override val type: HttpMessageType
        get() = error("not implemented")
      override val version: HttpVersion
        get() = error("not implemented")
      override val headers: MutableHttpHeaders
        get() = error("not implemented")
      override val body: MutableHttpPayload
        get() = error("not implemented")
    }.mutable)
  }
}
