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
import io.grpc.health.v1.HealthCheckRequest
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for the main [GrpcWebController]. */
@MicronautTest
class GrpcWebControllerTest: GrpcWebBaseTest() {
  // -- Basic Tests: DI -- //

  @Test fun testInjectableWhenEnabled() {
    assertNotNull(
      controller(),
      "GrpcWebController should be injectable"
    )
  }

  @Test fun testNotInjectableByDefault() {
    assertThrows<Throwable> {
      beanContext.getBean(
        GrpcWebController::class.java
      )
    }
  }

  // -- Tests: Malformed Requests -- //

  @Test fun testGrpcWebDisabled() {
    val req = HttpRequest.POST(
      "/_/rpc/some.cool.service/Method",
      ByteArray(0)
    )
    req.headers.set("content-type", "application/grpc-web+proto")
    req.headers.set("grpc-web", "1")
    val ctr = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = false
    })
    val response = runBlocking {
      ctr.handleRequest(
        "some.cool.service",
        "Method",
        req,
        null,
      )
    }
    assertNotNull(
      response,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.NOT_FOUND,
      response.status,
      "RPC controller should yield NOT_FOUND when gRPC web integration is disabled"
    )
  }

  @Test fun testMissingContentType() {
    val req = HttpRequest.POST(
      "/_/rpc/some.cool.service/Method",
      ByteArray(0)
    )
    val ctr = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
    })

    val response = runBlocking {
      ctr.handleRequest(
        "some.cool.service",
        "Method",
        req,
        null
      )
    }
    assertNotNull(
      response,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response.status,
      "RPC controller should yield BAD_REQUEST when content type is invalid"
    )
  }

  @Test fun testDisallowedContentType() {
    val req = HttpRequest.POST(
      "/_/rpc/some.cool.service/Method",
      ByteArray(0)
    )
    req.contentType(
      MediaType.TEXT_PLAIN
    )
    val ctr = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
    })

    val response = runBlocking {
      ctr.handleRequest(
        "some.cool.service",
        "Method",
        req,
        null
      )
    }
    assertNotNull(
      response,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response.status,
      "RPC controller should yield BAD_REQUEST when content type is invalid"
    )
  }

  @Test fun testMissingSentinelHeader() {
    val req = HttpRequest.POST(
      "/_/rpc/some.cool.service/Method",
      ByteArray(0)
    )
    req.contentType(
      GrpcWebContentType.BINARY.mediaType()
    )
    val ctr = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
    })

    val response = runBlocking {
      ctr.handleRequest(
        "some.cool.service",
        "Method",
        req,
        null
      )
    }
    assertNotNull(
      response,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response.status,
      "RPC controller should yield BAD_REQUEST when gRPC-web sentinel header is missing"
    )
  }

  @Test fun testMissingEmptyServicePath() {
    val req = HttpRequest.POST(
      "/_/rpc/some.cool.service/Method",
      ByteArray(0)
    )
    req.headers.set("content-type", "application/grpc-web+proto")
    val ctr = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
    })

    val response = runBlocking {
      ctr.handleRequest(
        "",
        "Method",
        req,
        null
      )
    }
    assertNotNull(
      response,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response.status,
      "RPC controller should yield BAD_REQUEST when gRPC-web service path is missing"
    )

    val response2 = runBlocking {
      ctr.handleRequest(
        " ",
        "Method",
        req,
        null
      )
    }
    assertNotNull(
      response2,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response2.status,
      "RPC controller should yield BAD_REQUEST when gRPC-web service path is empty"
    )
  }

  @Test fun testMissingEmptyMethodName() {
    val req = HttpRequest.POST(
      "/_/rpc/some.cool.service/Method",
      ByteArray(0)
    )
    req.headers.set("content-type", "application/grpc-web+proto")
    val ctr = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
    })

    val response = runBlocking {
      ctr.handleRequest(
        "some.cool.service",
        "",
        req,
        null
      )
    }
    assertNotNull(
      response,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response.status,
      "RPC controller should yield BAD_REQUEST when gRPC-web method name is missing"
    )

    val response2 = runBlocking {
      ctr.handleRequest(
        "some.cool.service",
        " ",
        req,
        null
      )
    }
    assertNotNull(
      response2,
      "should always get a non-null response from `handleRequest`"
    )
    assertEquals(
      HttpStatus.BAD_REQUEST,
      response2.status,
      "RPC controller should yield BAD_REQUEST when gRPC-web method name is empty"
    )
  }

  @Test fun testInvokeMissingService() {
    val format = GrpcWebContentType.BINARY
    val controller = controller(object : GrpcWebConfig {
      override fun isEnabled(): Boolean = true
    })

    // submit the request, which should not throw
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        "some.service.that.doesnt.Exist",
        "Something",
        HealthCheckRequest.newBuilder()
          .setService("grpc.health.v1.Health")
          .build(),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` from `handleRequest` via gRPC Web controller"
    )
    validErrorResponse(
      expectedStatus = Status.UNIMPLEMENTED,
      format,
      response,
    )
  }
}
