//[jvm](../../../../index.md)/[elide.server.rpc.web](../../index.md)/[GrpcWebCallResponse](../index.md)/[Error](index.md)/[Error](-error.md)

# Error

[jvm]\
fun [Error](-error.md)(contentType: [GrpcWebContentType](../../-grpc-web-content-type/index.md), status: Status, cause: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)?, headers: Metadata, trailers: Metadata)

## Parameters

jvm

| | |
|---|---|
| status | Terminal status which should be assigned to this error state. Defaults to Status.INTERNAL. |
| contentType | Content type to use for this response. Defaults to [GrpcWebContentType.BINARY](../../-grpc-web-content-type/-b-i-n-a-r-y/index.md). |
| cause | Cause information for this error, if known. Defaults to `null`. |
| headers | Header metadata which was captured as part of this response cycle. |
| trailers | Response trailers to enclose which describe this error state, if available. |
