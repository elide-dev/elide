//[rpc-jvm](../../../../index.md)/[elide.server.rpc.web](../../index.md)/[GrpcWebCallResponse](../index.md)/[UnaryResponse](index.md)/[UnaryResponse](-unary-response.md)

# UnaryResponse

[jvm]\
fun [UnaryResponse](-unary-response.md)(contentType: [GrpcWebContentType](../../-grpc-web-content-type/index.md), payload: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), headers: Metadata, trailers: Metadata)

## Parameters

jvm

| | |
|---|---|
| payload | Serialized payload data which should be enclosed in the response. |
| headers | Header metadata which was captured as part of this response cycle. |
| trailers | Response trailers to enclose within the response payload sent to the invoking client. |
