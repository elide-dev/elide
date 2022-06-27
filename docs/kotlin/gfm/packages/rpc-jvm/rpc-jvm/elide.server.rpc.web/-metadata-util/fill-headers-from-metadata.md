//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MetadataUtil](index.md)/[fillHeadersFromMetadata](fill-headers-from-metadata.md)

# fillHeadersFromMetadata

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [fillHeadersFromMetadata](fill-headers-from-metadata.md)(metadata: Metadata, target: MutableHeaders)

Given a set of gRPC io.grpc.Metadata, compute a corresponding set of HTTP Headers and return them.

#### Return

HTTP headers from the provided [metadata](fill-headers-from-metadata.md).

## Parameters

jvm

| | |
|---|---|
| metadata | gRPC metadata to convert into HTTP headers. |
| target | Headers which should receive the converted results. |
