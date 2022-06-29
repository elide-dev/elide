//[frontend](../../../index.md)/[lib.protobuf](../index.md)/[BinaryEncoder](index.md)

# BinaryEncoder

[js]\
open external class [BinaryEncoder](index.md)

## Constructors

| | |
|---|---|
| [BinaryEncoder](-binary-encoder.md) | [js]<br>fun [BinaryEncoder](-binary-encoder.md)() |

## Functions

| Name | Summary |
|---|---|
| [end](end.md) | [js]<br>open fun [end](end.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt; |
| [length](length.md) | [js]<br>open fun [length](length.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [writeBool](write-bool.md) | [js]<br>open fun [writeBool](write-bool.md)(value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [writeBytes](write-bytes.md) | [js]<br>open fun [writeBytes](write-bytes.md)(bytes: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html)) |
| [writeDouble](write-double.md) | [js]<br>open fun [writeDouble](write-double.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeEnum](write-enum.md) | [js]<br>open fun [writeEnum](write-enum.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeFixedHash64](write-fixed-hash64.md) | [js]<br>open fun [writeFixedHash64](write-fixed-hash64.md)(hash: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [writeFloat](write-float.md) | [js]<br>open fun [writeFloat](write-float.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeInt16](write-int16.md) | [js]<br>open fun [writeInt16](write-int16.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeInt32](write-int32.md) | [js]<br>open fun [writeInt32](write-int32.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeInt64](write-int64.md) | [js]<br>open fun [writeInt64](write-int64.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeInt64String](write-int64-string.md) | [js]<br>open fun [writeInt64String](write-int64-string.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [writeInt8](write-int8.md) | [js]<br>open fun [writeInt8](write-int8.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeSignedVarint32](write-signed-varint32.md) | [js]<br>open fun [writeSignedVarint32](write-signed-varint32.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeSignedVarint64](write-signed-varint64.md) | [js]<br>open fun [writeSignedVarint64](write-signed-varint64.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeSplitFixed64](write-split-fixed64.md) | [js]<br>open fun [writeSplitFixed64](write-split-fixed64.md)(lowBits: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), highBits: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeSplitVarint64](write-split-varint64.md) | [js]<br>open fun [writeSplitVarint64](write-split-varint64.md)(lowBits: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), highBits: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeString](write-string.md) | [js]<br>open fun [writeString](write-string.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [writeUint16](write-uint16.md) | [js]<br>open fun [writeUint16](write-uint16.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeUint32](write-uint32.md) | [js]<br>open fun [writeUint32](write-uint32.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeUint64](write-uint64.md) | [js]<br>open fun [writeUint64](write-uint64.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeUint8](write-uint8.md) | [js]<br>open fun [writeUint8](write-uint8.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeUnsignedVarint32](write-unsigned-varint32.md) | [js]<br>open fun [writeUnsignedVarint32](write-unsigned-varint32.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeUnsignedVarint64](write-unsigned-varint64.md) | [js]<br>open fun [writeUnsignedVarint64](write-unsigned-varint64.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeVarintHash64](write-varint-hash64.md) | [js]<br>open fun [writeVarintHash64](write-varint-hash64.md)(hash: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [writeZigzagVarint32](write-zigzag-varint32.md) | [js]<br>open fun [writeZigzagVarint32](write-zigzag-varint32.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeZigzagVarint64](write-zigzag-varint64.md) | [js]<br>open fun [writeZigzagVarint64](write-zigzag-varint64.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [writeZigzagVarint64String](write-zigzag-varint64-string.md) | [js]<br>open fun [writeZigzagVarint64String](write-zigzag-varint64-string.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
