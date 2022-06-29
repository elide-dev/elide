//[frontend](../../../index.md)/[lib.protobuf](../index.md)/[BinaryDecoder](index.md)

# BinaryDecoder

[js]\
open external class [BinaryDecoder](index.md)

## Constructors

| | |
|---|---|
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [ArrayBuffer](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-array-buffer/index.html) = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)() |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [ArrayBuffer](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-array-buffer/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [ArrayBuffer](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-array-buffer/index.html) = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html) = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html) = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt; = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt; = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt; = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = definedExternally) |
| [BinaryDecoder](-binary-decoder.md) | [js]<br>fun [BinaryDecoder](-binary-decoder.md)(bytes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = definedExternally, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [js]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [advance](advance.md) | [js]<br>open fun [advance](advance.md)(count: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [atEnd](at-end.md) | [js]<br>open fun [atEnd](at-end.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [clear](clear.md) | [js]<br>open fun [clear](clear.md)() |
| [clone](clone.md) | [js]<br>open fun [clone](clone.md)(): [BinaryDecoder](index.md) |
| [free](free.md) | [js]<br>open fun [free](free.md)() |
| [getBuffer](get-buffer.md) | [js]<br>open fun [getBuffer](get-buffer.md)(): [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html) |
| [getCursor](get-cursor.md) | [js]<br>open fun [getCursor](get-cursor.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [getEnd](get-end.md) | [js]<br>open fun [getEnd](get-end.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [getError](get-error.md) | [js]<br>open fun [getError](get-error.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [pastEnd](past-end.md) | [js]<br>open fun [pastEnd](past-end.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [readBool](read-bool.md) | [js]<br>open fun [readBool](read-bool.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [readBytes](read-bytes.md) | [js]<br>open fun [readBytes](read-bytes.md)(length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)): [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html) |
| [readDouble](read-double.md) | [js]<br>open fun [readDouble](read-double.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readEnum](read-enum.md) | [js]<br>open fun [readEnum](read-enum.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readFixedHash64](read-fixed-hash64.md) | [js]<br>open fun [readFixedHash64](read-fixed-hash64.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readFloat](read-float.md) | [js]<br>open fun [readFloat](read-float.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readInt16](read-int16.md) | [js]<br>open fun [readInt16](read-int16.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readInt32](read-int32.md) | [js]<br>open fun [readInt32](read-int32.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readInt64](read-int64.md) | [js]<br>open fun [readInt64](read-int64.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readInt64String](read-int64-string.md) | [js]<br>open fun [readInt64String](read-int64-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readInt8](read-int8.md) | [js]<br>open fun [readInt8](read-int8.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readSignedVarint32](read-signed-varint32.md) | [js]<br>open fun [readSignedVarint32](read-signed-varint32.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readSignedVarint32String](read-signed-varint32-string.md) | [js]<br>open fun [readSignedVarint32String](read-signed-varint32-string.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readSignedVarint64](read-signed-varint64.md) | [js]<br>open fun [readSignedVarint64](read-signed-varint64.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readSignedVarint64String](read-signed-varint64-string.md) | [js]<br>open fun [readSignedVarint64String](read-signed-varint64-string.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readString](read-string.md) | [js]<br>open fun [readString](read-string.md)(length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readStringWithLength](read-string-with-length.md) | [js]<br>open fun [readStringWithLength](read-string-with-length.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readUint16](read-uint16.md) | [js]<br>open fun [readUint16](read-uint16.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUint32](read-uint32.md) | [js]<br>open fun [readUint32](read-uint32.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUint64](read-uint64.md) | [js]<br>open fun [readUint64](read-uint64.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUint64String](read-uint64-string.md) | [js]<br>open fun [readUint64String](read-uint64-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readUint8](read-uint8.md) | [js]<br>open fun [readUint8](read-uint8.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUnsignedVarint32](read-unsigned-varint32.md) | [js]<br>open fun [readUnsignedVarint32](read-unsigned-varint32.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUnsignedVarint32String](read-unsigned-varint32-string.md) | [js]<br>open fun [readUnsignedVarint32String](read-unsigned-varint32-string.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUnsignedVarint64](read-unsigned-varint64.md) | [js]<br>open fun [readUnsignedVarint64](read-unsigned-varint64.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readUnsignedVarint64String](read-unsigned-varint64-string.md) | [js]<br>open fun [readUnsignedVarint64String](read-unsigned-varint64-string.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readVarintHash64](read-varint-hash64.md) | [js]<br>open fun [readVarintHash64](read-varint-hash64.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [readZigzagVarint32](read-zigzag-varint32.md) | [js]<br>open fun [readZigzagVarint32](read-zigzag-varint32.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readZigzagVarint64](read-zigzag-varint64.md) | [js]<br>open fun [readZigzagVarint64](read-zigzag-varint64.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [readZigzagVarint64String](read-zigzag-varint64-string.md) | [js]<br>open fun [readZigzagVarint64String](read-zigzag-varint64-string.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [reset](reset.md) | [js]<br>open fun [reset](reset.md)() |
| [setBlock](set-block.md) | [js]<br>open fun [setBlock](set-block.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt;)<br>open fun [setBlock](set-block.md)(data: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>open fun [setBlock](set-block.md)(data: [ArrayBuffer](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-array-buffer/index.html))<br>open fun [setBlock](set-block.md)(data: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html))<br>open fun [setBlock](set-block.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt;, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [ArrayBuffer](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-array-buffer/index.html), start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html), start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)&gt;, start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [ArrayBuffer](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-array-buffer/index.html), start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally)<br>open fun [setBlock](set-block.md)(data: [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html), start: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally, length: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) = definedExternally) |
| [setCursor](set-cursor.md) | [js]<br>open fun [setCursor](set-cursor.md)(cursor: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [setEnd](set-end.md) | [js]<br>open fun [setEnd](set-end.md)(end: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [skipVarint](skip-varint.md) | [js]<br>open fun [skipVarint](skip-varint.md)() |
| [unskipVarint](unskip-varint.md) | [js]<br>open fun [unskipVarint](unskip-varint.md)(value: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
