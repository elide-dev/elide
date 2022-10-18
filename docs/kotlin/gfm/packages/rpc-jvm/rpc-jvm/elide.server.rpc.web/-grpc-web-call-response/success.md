//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebCallResponse](index.md)/[success](success.md)

# success

[jvm]\
val [success](success.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

#### Parameters

jvm

| | |
|---|---|
| success | Indicates whether the call was encountered an error, in which case the value will be `false`, and the implementation will be [Error](-error/index.md), or a successful response, in which case the value will be `true` and the implementation will be an instance of [UnaryResponse](-unary-response/index.md). |
