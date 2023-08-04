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
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.grpc.stub.StreamObserver
import io.micronaut.grpc.annotation.GrpcService

/** Test-side implementation of a health service. */
@GrpcService class TestHealthService: HealthGrpc.HealthImplBase() {
  companion object {
    const val SERVING_SIGNAL = "service.ShouldBeServing"
    const val NOT_SERVING_SIGNAL = "service.ShouldNotBeServing"
    const val UNKNOWN_SIGNAL = "service.ShouldBeUnknown"
    const val ERR_SIGNAL = "service.ShouldCauseError"
  }

  override fun check(request: HealthCheckRequest, responseObserver: StreamObserver<HealthCheckResponse>) {
    val status = when (request.service) {
      SERVING_SIGNAL -> HealthCheckResponse.ServingStatus.SERVING
      NOT_SERVING_SIGNAL -> HealthCheckResponse.ServingStatus.NOT_SERVING
      UNKNOWN_SIGNAL -> HealthCheckResponse.ServingStatus.UNKNOWN
      ERR_SIGNAL -> null
      else -> HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN
    }

    if (status == null) {
      responseObserver.onError(Status.FAILED_PRECONDITION.asRuntimeException())
    } else {
      responseObserver.onNext(
        HealthCheckResponse.newBuilder()
          .setStatus(status)
          .build()
      )
      responseObserver.onCompleted()
    }
  }
}
