//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebContentType](index.md)

# GrpcWebContentType

[jvm]\
enum [GrpcWebContentType](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[GrpcWebContentType](index.md)&gt; 

Describes the content types available for use with gRPC Web, including their corresponding HTTP symbols.

## Parameters

jvm

| | |
|---|---|
| symbol | HTTP `Content-Type` value corresponding to this format. |

## Entries

| | |
|---|---|
| [BINARY](-b-i-n-a-r-y/index.md) | [jvm]<br>[BINARY](-b-i-n-a-r-y/index.md)<br>Binary dispatch for gRPC-Web, potentially with Protocol Buffers. |
| [TEXT](-t-e-x-t/index.md) | [jvm]<br>[TEXT](-t-e-x-t/index.md)<br>Text (base64-encoded) dispatch for gRPC-Web, potentially with Protocol Buffers. |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [contentType](content-type.md) | [jvm]<br>fun [contentType](content-type.md)(proto: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Render an HTTP `Content-Type` string for the selected format with consideration made for use of [proto](content-type.md)col buffers. |
| [mediaType](media-type.md) | [jvm]<br>fun [mediaType](media-type.md)(proto: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true): MediaType<br>Render a Micronaut MediaType for the selected format with consideration made for the use of [proto](media-type.md)col buffers. |

## Properties

| Name | Summary |
|---|---|
| [name](../-rpc-symbol/-t-r-a-i-l-e-r/index.md#-372974862%2FProperties%2F594929262) | [jvm]<br>val [name](../-rpc-symbol/-t-r-a-i-l-e-r/index.md#-372974862%2FProperties%2F594929262): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ordinal](../-rpc-symbol/-t-r-a-i-l-e-r/index.md#-739389684%2FProperties%2F594929262) | [jvm]<br>val [ordinal](../-rpc-symbol/-t-r-a-i-l-e-r/index.md#-739389684%2FProperties%2F594929262): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
