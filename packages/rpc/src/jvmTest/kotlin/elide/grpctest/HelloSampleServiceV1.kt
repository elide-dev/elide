package elide.grpctest

import io.micronaut.grpc.annotation.GrpcService

/** Sample service implementation in pure Kotlin. */
@GrpcService class HelloSampleServiceV1: HelloServiceGrpcKt.HelloServiceCoroutineImplBase() {
  override suspend fun renderMessage(request: Nopackage.HelloRequest): Nopackage.HelloResponse {
    return helloResponse {
      message = "Hello, ${request.name.ifBlank { "Elide" }}!"
    }
  }
}
