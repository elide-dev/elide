//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MetadataUtil](index.md)/[packTrailers](pack-trailers.md)

# packTrailers

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [packTrailers](pack-trailers.md)(stream: [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), trailers: Metadata)

Given a set of [trailers](pack-trailers.md) as gRPC Metadata, and the provided string [stream](pack-trailers.md), pack the present set of trailers into the response in a manner consistent with the gRPC Web Protocol.

Trailers are packed at the end of a given response, formatted as a set of `key:value` pairs, with each pair separated by `\r\n`. A special [RpcSymbol](../-rpc-symbol/index.md) denotes the `TRAILER` section ([RpcSymbol.TRAILER](../-rpc-symbol/-t-r-a-i-l-e-r/index.md)), and separates it from the `DATA` section ([RpcSymbol.DATA](../-rpc-symbol/-d-a-t-a/index.md)).

#### Parameters

jvm

| | |
|---|---|
| stream | Byte stream which should receive the packed trailers. |
| trailers | Set of trailers to pack into the provided [stream](pack-trailers.md). |
