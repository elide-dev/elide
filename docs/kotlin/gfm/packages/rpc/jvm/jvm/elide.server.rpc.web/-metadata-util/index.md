//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MetadataUtil](index.md)

# MetadataUtil

[jvm]\
object [MetadataUtil](index.md)

Provides metadata-related declarations and tooling for gRPC and gRPC Web.

## Functions

| Name | Summary |
|---|---|
| [fillHeadersFromMetadata](fill-headers-from-metadata.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [fillHeadersFromMetadata](fill-headers-from-metadata.md)(metadata: Metadata, target: MutableHeaders)<br>Given a set of gRPC io.grpc.Metadata, compute a corresponding set of HTTP Headers and return them. |
| [metadataFromHeaders](metadata-from-headers.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [metadataFromHeaders](metadata-from-headers.md)(headers: Headers): Metadata<br>Given a set of [headers](metadata-from-headers.md) from a generic HTTP or gRPC request, determine a corresponding set of gRPC call io.grpc.Metadata. |
| [packTrailer](pack-trailer.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [packTrailer](pack-trailer.md)(stream: [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), value: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html))<br>Given a raw [name](pack-trailer.md)/[value](pack-trailer.md) pair which should be used as a trailer in a gRPC Web response, pack them together in a manner consistent with the gRPC Web Protocol, and add them to the provided [stream](pack-trailer.md). |
| [packTrailers](pack-trailers.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [packTrailers](pack-trailers.md)(stream: [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), trailers: Metadata)<br>Given a set of [trailers](pack-trailers.md) as gRPC Metadata, and the provided string [stream](pack-trailers.md), pack the present set of trailers into the response in a manner consistent with the gRPC Web Protocol. |
