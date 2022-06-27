//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebCallResponse](index.md)

# GrpcWebCallResponse

[jvm]\
sealed class [GrpcWebCallResponse](index.md)

Describes the possible states that a gRPC Web call outcome can take on, namely an error state and a success state which provides one or more response payloads.

This class works in concert with [GrpcWebCall](../../../../../packages/rpc-jvm/elide.server.rpc.web/-grpc-web-call/index.md) to track the status of calls to the backing gRPC server. If a [GrpcWebCall](../../../../../packages/rpc-jvm/elide.server.rpc.web/-grpc-web-call/index.md) has a [GrpcWebCallResponse](index.md) object present, the call has completed processing.

## Parameters

jvm

| | |
|---|---|
| success | Indicates whether the call was encountered an error, in which case the value will be `false`, and the implementation will be [Error](-error/index.md), or a successful response, in which case the value will be `true` and the implementation will be an instance of [UnaryResponse](-unary-response/index.md). |

## Types

| Name | Summary |
|---|---|
| [Error](-error/index.md) | [jvm]<br>data class [Error](-error/index.md)(val contentType: [GrpcWebContentType](../-grpc-web-content-type/index.md), val status: Status, val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?, val headers: Metadata, val trailers: Metadata) : [GrpcWebCallResponse](index.md)<br>Response structure which carries information for a gRPC Web call which encountered a fatal error, including the ultimate [status](-error/status.md) and any [cause](-error/cause.md) information. |
| [UnaryResponse](-unary-response/index.md) | [jvm]<br>data class [UnaryResponse](-unary-response/index.md)(val contentType: [GrpcWebContentType](../-grpc-web-content-type/index.md), val payload: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), val headers: Metadata, val trailers: Metadata) : [GrpcWebCallResponse](index.md)<br>Response structure which carries information for a gRPC Web call which completed and yielded one or more response [payload](-unary-response/payload.md) structures, along with any extra [trailers](-unary-response/trailers.md) that should be sent. |

## Functions

| Name | Summary |
|---|---|
| [fill](fill.md) | [jvm]<br>abstract fun [fill](fill.md)(response: MutableHttpResponse&lt;[RawRpcPayload](../index.md#-571776252%2FClasslikes%2F-814346341)&gt;)<br>Fill out the provided HTTP [response](fill.md) with data attached to this call response state; the response is expected to comply with the structure stipulated by the [gRPC Web Protocol](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md) document. |

## Properties

| Name | Summary |
|---|---|
| [success](success.md) | [jvm]<br>val [success](success.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

## Inheritors

| Name |
|---|
| [Error](-error/index.md) |
| [UnaryResponse](-unary-response/index.md) |
