//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MetadataUtil](index.md)/[metadataFromHeaders](metadata-from-headers.md)

# metadataFromHeaders

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [metadataFromHeaders](metadata-from-headers.md)(headers: Headers): Metadata

Given a set of [headers](metadata-from-headers.md) from a generic HTTP or gRPC request, determine a corresponding set of gRPC call io.grpc.Metadata.

#### Return

gRPC metadata decoded from the provided [headers](metadata-from-headers.md).

## Parameters

jvm

| | |
|---|---|
| headers | Headers to decode into metadata. |
