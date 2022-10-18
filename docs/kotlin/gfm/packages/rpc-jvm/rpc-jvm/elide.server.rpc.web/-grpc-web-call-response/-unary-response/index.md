//[rpc-jvm](../../../../index.md)/[elide.server.rpc.web](../../index.md)/[GrpcWebCallResponse](../index.md)/[UnaryResponse](index.md)

# UnaryResponse

[jvm]\
data class [UnaryResponse](index.md)(val contentType: [GrpcWebContentType](../../-grpc-web-content-type/index.md), val payload: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), val headers: Metadata, val trailers: Metadata) : [GrpcWebCallResponse](../index.md)

Response structure which carries information for a gRPC Web call which completed and yielded one or more response [payload](payload.md) structures, along with any extra [trailers](trailers.md) that should be sent.

No status is accepted because it is assumed to be Status.OK. For all other statuses, use an [Error](../-error/index.md).

#### Parameters

jvm

| | |
|---|---|
| payload | Serialized payload data which should be enclosed in the response. |
| headers | Header metadata which was captured as part of this response cycle. |
| trailers | Response trailers to enclose within the response payload sent to the invoking client. |

## Constructors

| | |
|---|---|
| [UnaryResponse](-unary-response.md) | [jvm]<br>fun [UnaryResponse](-unary-response.md)(contentType: [GrpcWebContentType](../../-grpc-web-content-type/index.md), payload: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), headers: Metadata, trailers: Metadata) |

## Functions

| Name | Summary |
|---|---|
| [fill](fill.md) | [jvm]<br>open override fun [fill](fill.md)(response: MutableHttpResponse&lt;[RawRpcPayload](../../index.md#-571776252%2FClasslikes%2F-814346341)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [contentType](content-type.md) | [jvm]<br>val [contentType](content-type.md): [GrpcWebContentType](../../-grpc-web-content-type/index.md) |
| [headers](headers.md) | [jvm]<br>val [headers](headers.md): Metadata |
| [payload](payload.md) | [jvm]<br>val [payload](payload.md): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) |
| [success](../success.md) | [jvm]<br>val [success](../success.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [trailers](trailers.md) | [jvm]<br>val [trailers](trailers.md): Metadata |
