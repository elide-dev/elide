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

package elide.http

import io.netty.handler.codec.http.DefaultHttpHeaders
import kotlin.test.*
import elide.http.headers.NettyHttpHeaders

class PureMessagesTest {
  @Test fun testCreateResponse() {
    val headerMap = mapOf(
      "sample" to listOf("Hello"),
      "multi" to listOf("One", "Two"),
    )
    val response = Response.of(
      ProtocolVersion.HTTP_1_1,
      Status.Ok,
      NettyHttpHeaders(DefaultHttpHeaders().apply {
        headerMap.forEach { (key, values) ->
          values.forEach { value ->
            add(key, value)
          }
        }
      }),
    )
    assertNotNull(response)
    assertNotNull(response.headers)
    assertNotNull(response.body)
    assertNotNull(response.status)
    assertNotNull(response.version)
    assertNotNull(response.type)
    assertNull(response.trailers)
    val mut = response.toMutable()
    assertNotNull(mut)
    assertNotNull(mut.headers)
    assertNotNull(mut.body)
    assertNotNull(mut.status)
    assertNotNull(mut.version)
    assertNotNull(mut.type)
    val other = mut.toMutable()
    assertSame(mut, other)
    assertNotNull(response.toString())
    assertNotNull(mut.toString())
    assertEquals(response, response)
    mut.status = Status.NotFound
    val built = mut.build()
    assertNotNull(built)
    assertNotNull(built.headers)
    assertNotNull(built.body)
    assertNotNull(built.status)
    assertNotNull(built.version)
    assertNotNull(built.type)
    assertNotNull(built.toString())
    assertNotNull(built.headers.toString())
    assertNull(response.trailers)
    assertEquals(404u, built.status.code.symbol)
  }

  @Test fun testCreateResponseWithTrailers() {
    val headerMap = mapOf(
      "sample" to listOf("Hello"),
      "multi" to listOf("One", "Two"),
    )
    val response = Response.of(
      ProtocolVersion.HTTP_1_1,
      Status.Ok,
      NettyHttpHeaders(DefaultHttpHeaders().apply {
        headerMap.forEach { (key, values) ->
          values.forEach { value ->
            add(key, value)
          }
        }
      }),
      trailers = NettyHttpHeaders(DefaultHttpHeaders().apply {
        headerMap.forEach { (key, values) ->
          values.forEach { value ->
            add(key, value)
          }
        }
      }),
    )
    assertNotNull(response)
    assertNotNull(response.headers)
    assertNotNull(response.body)
    assertNotNull(response.status)
    assertNotNull(response.version)
    assertNotNull(response.type)
    assertNotNull(response.trailers)
    val mut = response.toMutable()
    assertNotNull(mut)
    assertNotNull(mut.headers)
    assertNotNull(mut.body)
    assertNotNull(mut.status)
    assertNotNull(mut.version)
    assertNotNull(mut.type)
    val other = mut.toMutable()
    assertSame(mut, other)
    assertNotNull(response.toString())
    assertNotNull(mut.toString())
    assertEquals(response, response)
    mut.status = Status.NotFound
    val built = mut.build()
    assertNotNull(built)
    assertNotNull(built.headers)
    assertNotNull(built.body)
    assertNotNull(built.status)
    assertNotNull(built.version)
    assertNotNull(built.type)
    assertNotNull(built.toString())
    assertNotNull(built.headers.toString())
    assertNotNull(response.trailers)
    assertEquals(404u, built.status.code.symbol)
  }
}
