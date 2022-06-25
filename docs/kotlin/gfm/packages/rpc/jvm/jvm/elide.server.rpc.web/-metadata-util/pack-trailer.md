//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MetadataUtil](index.md)/[packTrailer](pack-trailer.md)

# packTrailer

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [packTrailer](pack-trailer.md)(stream: [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), value: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html))

Given a raw [name](pack-trailer.md)/[value](pack-trailer.md) pair which should be used as a trailer in a gRPC Web response, pack them together in a manner consistent with the gRPC Web Protocol, and add them to the provided [stream](pack-trailer.md).

This method works on raw strings, see [packTrailers](pack-trailers.md) for a method which works based on a full set of Metadata.

## Parameters

jvm

| | |
|---|---|
| stream | Byte stream which should receive the packed result. |
| name | Name of the trailer which we should add to the [stream](pack-trailer.md). |
| value | Value of the trailer which we should add to the [stream](pack-trailer.md). |
