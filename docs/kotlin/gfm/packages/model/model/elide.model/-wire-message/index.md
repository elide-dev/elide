//[model](../../../index.md)/[elide.model](../index.md)/[WireMessage](index.md)

# WireMessage

[common]\
expect open class [WireMessage](index.md)

Describes the expected interface for wire messages, usually implemented via Protocol Buffers on a given platform.

[js, jvm, native]\
actual open class [WireMessage](index.md)

Describes the expected interface for wire messages, usually implemented via Protocol Buffers on a given platform.

## Constructors

| | |
|---|---|
| [WireMessage](-wire-message.md) | [js, jvm, native]<br>fun [WireMessage](-wire-message.md)() |

## Functions

| Name | Summary |
|---|---|
| [getProto](../../../../../packages/model/model/elide.model/-wire-message/[native]get-proto.md) | [js, jvm, native]<br>[js]<br>fun [getProto]([js]get-proto.md)(): [Message](../../../../../packages/frontend/frontend/lib.protobuf/-message/index.md)<br>[jvm]<br>fun [getProto]([jvm]get-proto.md)(): Message<br>[native]<br>fun [getProto]([native]get-proto.md)(): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [toSerializedBytes](to-serialized-bytes.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [toSerializedBytes](to-serialized-bytes.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>[js, jvm, native]<br>actual open fun [toSerializedBytes](to-serialized-bytes.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Serialize this [WireMessage](index.md) instance into a raw [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), which is suitable for sending over the wire; formats expressed via this interface must keep schema in sync on both sides. |
| [toSerializedString](to-serialized-string.md) | [common, js, jvm, native]<br>[common]<br>expect open fun [toSerializedString](to-serialized-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>[js, jvm, native]<br>actual open fun [toSerializedString](to-serialized-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Return this [WireMessage](index.md) as a debug-friendly [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) representation, which emits property values and other info descriptive to the current [WireMessage](index.md) instance. |
