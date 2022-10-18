//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebContentType](index.md)/[contentType](content-type.md)

# contentType

[jvm]\
fun [contentType](content-type.md)(proto: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Render an HTTP `Content-Type` string for the selected format with consideration made for use of [proto](content-type.md)col buffers.

#### Return

`Content-Type` string to use.

#### Parameters

jvm

| | |
|---|---|
| proto | Whether protocol buffers are in use. |
