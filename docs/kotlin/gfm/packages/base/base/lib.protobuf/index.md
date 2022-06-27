//[base](../../index.md)/[lib.protobuf](index.md)

# Package lib.protobuf

## Types

| Name | Summary |
|---|---|
| [BinaryDecoder](-binary-decoder/index.md) | [js]<br>open external class [BinaryDecoder](-binary-decoder/index.md) |
| [BinaryEncoder](-binary-encoder/index.md) | [js]<br>open external class [BinaryEncoder](-binary-encoder/index.md) |
| [BinaryIterator](-binary-iterator/index.md) | [js]<br>open external class [BinaryIterator](-binary-iterator/index.md)(decoder: [BinaryDecoder](-binary-decoder/index.md) = definedExternally, next: () -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? = definedExternally, elements: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; = definedExternally) |
| [BinaryRead](index.md#-912733953%2FClasslikes%2F-431612152) | [js]<br>typealias [BinaryRead](index.md#-912733953%2FClasslikes%2F-431612152) = (msg: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), reader: [BinaryReadReader](index.md#102622972%2FClasslikes%2F-431612152)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [BinaryReadCallback](index.md#-1461984710%2FClasslikes%2F-431612152) | [js]<br>typealias [BinaryReadCallback](index.md#-1461984710%2FClasslikes%2F-431612152) = (value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), binaryReader: [BinaryReader](-binary-reader/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [BinaryReader](-binary-reader/index.md) | [js]<br>open external class [BinaryReader](-binary-reader/index.md) |
| [BinaryReadReader](index.md#102622972%2FClasslikes%2F-431612152) | [js]<br>typealias [BinaryReadReader](index.md#102622972%2FClasslikes%2F-431612152) = (msg: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), binaryReader: [BinaryReader](-binary-reader/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [BinaryWrite](index.md#-2100345842%2FClasslikes%2F-431612152) | [js]<br>typealias [BinaryWrite](index.md#-2100345842%2FClasslikes%2F-431612152) = (fieldNumber: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), writerCallback: [BinaryWriteCallback](index.md#1567219273%2FClasslikes%2F-431612152)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [BinaryWriteCallback](index.md#1567219273%2FClasslikes%2F-431612152) | [js]<br>typealias [BinaryWriteCallback](index.md#1567219273%2FClasslikes%2F-431612152) = (value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), binaryWriter: [BinaryWriter](-binary-writer/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [BinaryWriter](-binary-writer/index.md) | [js]<br>open external class [BinaryWriter](-binary-writer/index.md) |
| [ExtensionFieldBinaryInfo](-extension-field-binary-info/index.md) | [js]<br>open external class [ExtensionFieldBinaryInfo](-extension-field-binary-info/index.md)&lt;[T](-extension-field-binary-info/index.md)&gt;(fieldInfo: [ExtensionFieldInfo](-extension-field-info/index.md)&lt;[T](-extension-field-binary-info/index.md)&gt;, binaryReaderFn: [BinaryRead](index.md#-912733953%2FClasslikes%2F-431612152), binaryWriterFn: [BinaryWrite](index.md#-2100345842%2FClasslikes%2F-431612152), opt_binaryMessageSerializeFn: (msg: [Message](-message/index.md), writer: [BinaryWriter](-binary-writer/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), opt_binaryMessageDeserializeFn: (msg: [Message](-message/index.md), reader: [BinaryReader](-binary-reader/index.md)) -&gt; [Message](-message/index.md), opt_isPacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [ExtensionFieldInfo](-extension-field-info/index.md) | [js]<br>open external class [ExtensionFieldInfo](-extension-field-info/index.md)&lt;[T](-extension-field-info/index.md)&gt;(fieldIndex: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), fieldName: [T$2](-t$2/index.md), ctor: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), toObjectFn: [StaticToObject](index.md#-787487058%2FClasslikes%2F-431612152), isRepeated: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [FieldType](-field-type/index.md) | [js]<br>external enum [FieldType](-field-type/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[FieldType](-field-type/index.md)&gt; |
| [FieldValueArray](index.md#-1593235606%2FClasslikes%2F-431612152) | [js]<br>typealias [FieldValueArray](index.md#-1593235606%2FClasslikes%2F-431612152) = [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;dynamic&gt; |
| [Int64](-int64/index.md) | [js]<br>open external class [Int64](-int64/index.md)(lo: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), hi: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [Map](-map/index.md) | [js]<br>open external class [Map](-map/index.md)&lt;[K](-map/index.md), [V](-map/index.md)&gt;(arr: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, valueCtor: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) = definedExternally) |
| [Message](-message/index.md) | [js]<br>open external class [Message](-message/index.md) |
| [MessageArray](index.md#-185924924%2FClasslikes%2F-431612152) | [js]<br>typealias [MessageArray](index.md#-185924924%2FClasslikes%2F-431612152) = [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [StaticToObject](index.md#-787487058%2FClasslikes%2F-431612152) | [js]<br>typealias [StaticToObject](index.md#-787487058%2FClasslikes%2F-431612152) = (includeInstance: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), msg: [Message](-message/index.md)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [T$0](-t$0/index.md) | [js]<br>external interface [T$0](-t$0/index.md) |
| [T$1](-t$1/index.md) | [js]<br>external interface [T$1](-t$1/index.md) |
| [T$2](-t$2/index.md) | [js]<br>external interface [T$2](-t$2/index.md) |
| [UInt64](-u-int64/index.md) | [js]<br>open external class [UInt64](-u-int64/index.md)(lo: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), hi: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)) |
| [WireType](-wire-type/index.md) | [js]<br>external enum [WireType](-wire-type/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[WireType](-wire-type/index.md)&gt; |

## Properties

| Name | Summary |
|---|---|
| [FieldTypeToWireType](-field-type-to-wire-type.md) | [js]<br>external var [FieldTypeToWireType](-field-type-to-wire-type.md): (fieldType: [FieldType](-field-type/index.md)) -&gt; [WireType](-wire-type/index.md) |
| [FLOAT32_EPS](-f-l-o-a-t32_-e-p-s.md) | [js]<br>external var [FLOAT32_EPS](-f-l-o-a-t32_-e-p-s.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [FLOAT32_MAX](-f-l-o-a-t32_-m-a-x.md) | [js]<br>external var [FLOAT32_MAX](-f-l-o-a-t32_-m-a-x.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [FLOAT32_MIN](-f-l-o-a-t32_-m-i-n.md) | [js]<br>external var [FLOAT32_MIN](-f-l-o-a-t32_-m-i-n.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [FLOAT64_EPS](-f-l-o-a-t64_-e-p-s.md) | [js]<br>external var [FLOAT64_EPS](-f-l-o-a-t64_-e-p-s.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [FLOAT64_MAX](-f-l-o-a-t64_-m-a-x.md) | [js]<br>external var [FLOAT64_MAX](-f-l-o-a-t64_-m-a-x.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [FLOAT64_MIN](-f-l-o-a-t64_-m-i-n.md) | [js]<br>external var [FLOAT64_MIN](-f-l-o-a-t64_-m-i-n.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [INVALID_FIELD_NUMBER](-i-n-v-a-l-i-d_-f-i-e-l-d_-n-u-m-b-e-r.md) | [js]<br>external var [INVALID_FIELD_NUMBER](-i-n-v-a-l-i-d_-f-i-e-l-d_-n-u-m-b-e-r.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_20](-t-w-o_-t-o_20.md) | [js]<br>external var [TWO_TO_20](-t-w-o_-t-o_20.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_23](-t-w-o_-t-o_23.md) | [js]<br>external var [TWO_TO_23](-t-w-o_-t-o_23.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_31](-t-w-o_-t-o_31.md) | [js]<br>external var [TWO_TO_31](-t-w-o_-t-o_31.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_32](-t-w-o_-t-o_32.md) | [js]<br>external var [TWO_TO_32](-t-w-o_-t-o_32.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_52](-t-w-o_-t-o_52.md) | [js]<br>external var [TWO_TO_52](-t-w-o_-t-o_52.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_63](-t-w-o_-t-o_63.md) | [js]<br>external var [TWO_TO_63](-t-w-o_-t-o_63.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [TWO_TO_64](-t-w-o_-t-o_64.md) | [js]<br>external var [TWO_TO_64](-t-w-o_-t-o_64.md): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [ZERO_HASH](-z-e-r-o_-h-a-s-h.md) | [js]<br>external var [ZERO_HASH](-z-e-r-o_-h-a-s-h.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
