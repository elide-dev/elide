//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MessageDeserializer](index.md)

# MessageDeserializer

[jvm]\
interface [MessageDeserializer](index.md)

Defines the API surface provided for a class which knows how to deserialize gRPC method parameters from raw bytes to inflated com.google.protobuf.Message instances.

## Functions

| Name | Summary |
|---|---|
| [deserialize](deserialize.md) | [jvm]<br>abstract fun [deserialize](deserialize.md)(method: [Method](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Method.html), rawData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): Message<br>Deserialize a generic protocol buffer Message from the provided set of [rawData](deserialize.md), with the intent of dispatching the provided RPC [method](deserialize.md). |
