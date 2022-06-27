package elide.grpctest

import elide.server.rpc.web.GrpcWeb
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.grpc.annotation.GrpcService
import java.nio.charset.StandardCharsets

/** Implementation of the test sample service for error case testing via the gRPC Web layer. */
@GrpcService class TestSampleServiceV1: SampleServiceGrpc.SampleServiceImplBase() {
  override fun methodThatTakesTooLong(
    request: Sample.SampleRequest,
    responseObserver: StreamObserver<Sample.SampleResponse>
  ) {
    try {
      Thread.sleep(
        3 * 1000
      )
      responseObserver.onNext(
        Sample.SampleResponse.newBuilder()
          .setMessage("Hello, ${request.name.ifBlank { "World" }}!")
          .build()
      )
      responseObserver.onCompleted()
    } catch (interrupt: InterruptedException) {
      Thread.interrupted()
      responseObserver.onError(
        Status.DEADLINE_EXCEEDED.asRuntimeException()
      )
    }
  }

  override fun methodThatErrors(
    request: Sample.StatusRequest,
    responseObserver: StreamObserver<Sample.SampleResponse>
  ) {
    responseObserver.onError(
      Status.fromCodeValue(request.statusCode).asRuntimeException()
    )
  }

  override fun methodWithTrailers(
    request: Sample.SampleRequest,
    responseObserver: StreamObserver<Sample.SampleResponse>
  ) {
    val trailers = Metadata()
    trailers.put(
      GrpcWeb.Metadata.trace,
      "some trace value here"
    )
    val binaryHeader = Metadata.Key.of(
      "some-binary-header-bin",
      Metadata.BINARY_BYTE_MARSHALLER
    )
    trailers.put(
      binaryHeader,
      "hello binary header".toByteArray(StandardCharsets.UTF_8)
    )
    responseObserver.onError(
      Status.FAILED_PRECONDITION.withDescription(
        "Error description"
      ).asRuntimeException(
        trailers
      )
    )
  }

  override fun methodWithFatalError(
    request: Sample.SampleRequest,
    responseObserver: StreamObserver<Sample.SampleResponse>
  ) {
    throw IllegalStateException(
      "This is a simulated fatal error"
    )
  }
}
