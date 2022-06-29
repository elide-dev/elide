//[frontend](../../../../index.md)/[lib.protobuf](../../index.md)/[Map](../index.md)/[Companion](index.md)

# Companion

[js]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [deserializeBinary](deserialize-binary.md) | [js]<br>fun &lt;[K](deserialize-binary.md), [V](deserialize-binary.md)&gt; [deserializeBinary](deserialize-binary.md)(map: [Map](../index.md)&lt;[K](deserialize-binary.md), [V](deserialize-binary.md)&gt;, reader: [BinaryReader](../../-binary-reader/index.md), keyReaderFn: (reader: [BinaryReader](../../-binary-reader/index.md)) -&gt; [K](deserialize-binary.md), valueReaderFn: (reader: [BinaryReader](../../-binary-reader/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [BinaryReadCallback](../../index.md#-1461984710%2FClasslikes%2F2039821458)) -&gt; [V](deserialize-binary.md), readCallback: [BinaryReadCallback](../../index.md#-1461984710%2FClasslikes%2F2039821458) = definedExternally, defaultKey: [K](deserialize-binary.md) = definedExternally, defaultValue: [V](deserialize-binary.md) = definedExternally) |
| [fromObject](from-object.md) | [js]<br>fun &lt;[TK](from-object.md), [TV](from-object.md)&gt; [fromObject](from-object.md)(entries: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, valueCtor: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), valueFromObject: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): [Map](../index.md)&lt;[TK](from-object.md), [TV](from-object.md)&gt; |
