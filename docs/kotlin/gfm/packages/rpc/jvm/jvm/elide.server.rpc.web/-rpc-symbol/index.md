//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[RpcSymbol](index.md)

# RpcSymbol

[jvm]\
enum [RpcSymbol](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[RpcSymbol](index.md)&gt; 

Enumerates byte symbols which are meaningful in the gRPC-Web Protocol; there are only two, [DATA](-d-a-t-a/index.md) and [TRAILER](-t-r-a-i-l-e-r/index.md).

The [TRAILER](-t-r-a-i-l-e-r/index.md) value is used to encode gRPC responses over regular HTTP/1.1-style responses, if needed. The [DATA](-d-a-t-a/index.md) symbol is used to demarcate a data frame inside a gRPC Web request or response.

## Entries

| | |
|---|---|
| [DATA](-d-a-t-a/index.md) | [jvm]<br>[DATA](-d-a-t-a/index.md)<br>Symbol indicating a data frame. |
| [TRAILER](-t-r-a-i-l-e-r/index.md) | [jvm]<br>[TRAILER](-t-r-a-i-l-e-r/index.md)<br>Symbol used to demarcate trailers. |

## Properties

| Name | Summary |
|---|---|
| [name](-t-r-a-i-l-e-r/index.md#-372974862%2FProperties%2F594929262) | [jvm]<br>val [name](-t-r-a-i-l-e-r/index.md#-372974862%2FProperties%2F594929262): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ordinal](-t-r-a-i-l-e-r/index.md#-739389684%2FProperties%2F594929262) | [jvm]<br>val [ordinal](-t-r-a-i-l-e-r/index.md#-739389684%2FProperties%2F594929262): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [value](value.md) | [jvm]<br>val [value](value.md): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte/index.html) |
