//[base](../../../../index.md)/[lib.protobuf](../../index.md)/[Map](../index.md)/[Companion](index.md)/[deserializeBinary](deserialize-binary.md)

# deserializeBinary

[js]\
fun &lt;[K](deserialize-binary.md), [V](deserialize-binary.md)&gt; [deserializeBinary](deserialize-binary.md)(map: [Map](../index.md)&lt;[K](deserialize-binary.md), [V](deserialize-binary.md)&gt;, reader: [BinaryReader](../../-binary-reader/index.md), keyReaderFn: (reader: [BinaryReader](../../-binary-reader/index.md)) -&gt; [K](deserialize-binary.md), valueReaderFn: (reader: [BinaryReader](../../-binary-reader/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [BinaryReadCallback](../../index.md#-1461984710%2FClasslikes%2F-431612152)) -&gt; [V](deserialize-binary.md), readCallback: [BinaryReadCallback](../../index.md#-1461984710%2FClasslikes%2F-431612152) = definedExternally, defaultKey: [K](deserialize-binary.md) = definedExternally, defaultValue: [V](deserialize-binary.md) = definedExternally)
