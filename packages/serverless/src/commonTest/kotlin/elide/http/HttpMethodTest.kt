/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.http.api.StandardHttpMethod

/** Tests for HTTP method definitions and classes. */
class HttpMethodTest {
  @Test fun testHttpMethodNames() {
    StandardHttpMethod.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertNotNull(it.write)
      assertNotNull(it.body)
      assertNotNull(it.idempotent)
      assertFalse(it.name.isBlank())
    }
  }

  @Test fun testHttpMethodWriteExpected() {
    StandardHttpMethod.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.write)
      if (StandardHttpMethod.writeMethods.contains(it)) {
        assertTrue(it.write)
      }
    }
  }

  @Test fun testHttpMethodReadExpected() {
    StandardHttpMethod.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.write)
      if (StandardHttpMethod.readMethods.contains(it)) {
        assertFalse(it.write)
      }
    }
  }

  @Test fun testReadMethods() {
    listOf(
      StandardHttpMethod.GET,
      StandardHttpMethod.HEAD,
      StandardHttpMethod.OPTIONS,
    ).forEach {
      assertNotNull(it)
      assertFalse(it.write)
    }
  }

  @Test fun testWriteMethods() {
    listOf(
      StandardHttpMethod.POST,
      StandardHttpMethod.PUT,
      StandardHttpMethod.DELETE,
      StandardHttpMethod.PATCH,
    ).forEach {
      assertNotNull(it)
      assertTrue(it.write)
    }
  }

  @Test fun testExoticMethods() {
    listOf(
      StandardHttpMethod.TRACE,
      StandardHttpMethod.CONNECT,
    ).forEach {
      assertNotNull(it)
      assertFalse(it.write)
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test fun testSerializer() {
    assertNotNull(StandardHttpMethod.serializer())
    StandardHttpMethod.entries.forEach {
      ProtoBuf.encodeToByteArray(StandardHttpMethod.serializer(), it)
    }
  }
}
