package elide.embedded.internal

import io.micronaut.context.annotation.Requires
import tools.elide.call.v1alpha1.UnaryInvocationRequest
import java.nio.ByteBuffer
import kotlinx.atomicfu.atomic
import elide.annotations.Singleton
import elide.embedded.*
import elide.embedded.http.EmbeddedResponse

@Singleton
@Requires(bean = EmbeddedConfiguration::class, beanProperty = "protocolFormat", value = "PROTOBUF")
internal class ProtobufCallCodec : EmbeddedCallCodec {
  private val nextId = atomic(0L)

  override fun decode(unsafe: UnsafeCall): EmbeddedCall {
    require(unsafe is ByteBuffer) { "In ProtoBuf mode, foreign calls must be passed as byte buffers." }

    val decoded = UnaryInvocationRequest.parseFrom(unsafe)
    require(decoded.hasFetch()) { "Only fetch invocations are currently supported" }

    val fetch = decoded.fetch

    val callId = EmbeddedCallId(nextId.getAndIncrement())
    val request = ImmediateRequest(
      uri = fetch.request.path,
      method = fetch.request.methodCase.name,
      headers = emptyMap(),
    )

    return EmbeddedCall(callId, request)
  }

  override fun encode(response: EmbeddedResponse): UnsafeResponse {
    TODO("Not yet implemented")
  }
}