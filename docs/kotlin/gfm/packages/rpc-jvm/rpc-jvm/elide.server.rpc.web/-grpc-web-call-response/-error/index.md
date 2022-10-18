//[rpc-jvm](../../../../index.md)/[elide.server.rpc.web](../../index.md)/[GrpcWebCallResponse](../index.md)/[Error](index.md)

# Error

[jvm]\
data class [Error](index.md)(val contentType: [GrpcWebContentType](../../-grpc-web-content-type/index.md), val status: Status, val cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?, val headers: Metadata, val trailers: Metadata) : [GrpcWebCallResponse](../index.md)

Response structure which carries information for a gRPC Web call which encountered a fatal error, including the ultimate [status](status.md) and any [cause](cause.md) information.

Space for [trailers](trailers.md) are provided, but using error trailers is completely optional.

#### Parameters

jvm

| | |
|---|---|
| status | Terminal status which should be assigned to this error state. Defaults to Status.INTERNAL. |
| contentType | Content type to use for this response. Defaults to [GrpcWebContentType.BINARY](../../-grpc-web-content-type/-b-i-n-a-r-y/index.md). |
| cause | Cause information for this error, if known. Defaults to `null`. |
| headers | Header metadata which was captured as part of this response cycle. |
| trailers | Response trailers to enclose which describe this error state, if available. |

## Constructors

| | |
|---|---|
| [Error](-error.md) | [jvm]<br>fun [Error](-error.md)(contentType: [GrpcWebContentType](../../-grpc-web-content-type/index.md), status: Status, cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?, headers: Metadata, trailers: Metadata) |

## Functions

| Name | Summary |
|---|---|
| [fill](fill.md) | [jvm]<br>open override fun [fill](fill.md)(response: MutableHttpResponse&lt;[RawRpcPayload](../../index.md#-571776252%2FClasslikes%2F-814346341)&gt;) |

## Properties

| Name | Summary |
|---|---|
| [cause](cause.md) | [jvm]<br>val [cause](cause.md): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)? |
| [contentType](content-type.md) | [jvm]<br>val [contentType](content-type.md): [GrpcWebContentType](../../-grpc-web-content-type/index.md) |
| [headers](headers.md) | [jvm]<br>val [headers](headers.md): Metadata |
| [status](status.md) | [jvm]<br>val [status](status.md): Status |
| [success](../success.md) | [jvm]<br>val [success](../success.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [trailers](trailers.md) | [jvm]<br>val [trailers](trailers.md): Metadata |
