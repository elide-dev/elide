//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[MessageDeserializer](index.md)/[deserialize](deserialize.md)

# deserialize

[jvm]\
abstract fun [deserialize](deserialize.md)(method: [Method](https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Method.html), rawData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): Message

Deserialize a generic protocol buffer Message from the provided set of [rawData](deserialize.md), with the intent of dispatching the provided RPC [method](deserialize.md).

If the method does not have the expected parameter, [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html) is thrown. If the data cannot be decoded, an exception ([IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html)) is thrown. Other errors, such as the parser being missing on the protocol buffer message, are thrown as [IllegalStateException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-state-exception/index.html).

#### Return

Resulting instance of Message decoded as a request for the provided [method](deserialize.md).

## Parameters

jvm

| | |
|---|---|
| method | Method to resolve the proto Message type from. The proto is expected to be the first parameter. |
| rawData | Raw data from the request which should be inflated into the resulting message type. |
