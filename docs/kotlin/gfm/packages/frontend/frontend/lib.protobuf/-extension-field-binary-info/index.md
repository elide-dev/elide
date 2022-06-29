//[frontend](../../../index.md)/[lib.protobuf](../index.md)/[ExtensionFieldBinaryInfo](index.md)

# ExtensionFieldBinaryInfo

[js]\
open external class [ExtensionFieldBinaryInfo](index.md)&lt;[T](index.md)&gt;(fieldInfo: [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](index.md)&gt;, binaryReaderFn: [BinaryRead](../index.md#-912733953%2FClasslikes%2F2039821458), binaryWriterFn: [BinaryWrite](../index.md#-2100345842%2FClasslikes%2F2039821458), opt_binaryMessageSerializeFn: (msg: [Message](../-message/index.md), writer: [BinaryWriter](../-binary-writer/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), opt_binaryMessageDeserializeFn: (msg: [Message](../-message/index.md), reader: [BinaryReader](../-binary-reader/index.md)) -&gt; [Message](../-message/index.md), opt_isPacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))

## Constructors

| | |
|---|---|
| [ExtensionFieldBinaryInfo](-extension-field-binary-info.md) | [js]<br>fun &lt;[T](index.md)&gt; [ExtensionFieldBinaryInfo](-extension-field-binary-info.md)(fieldInfo: [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](index.md)&gt;, binaryReaderFn: [BinaryRead](../index.md#-912733953%2FClasslikes%2F2039821458), binaryWriterFn: [BinaryWrite](../index.md#-2100345842%2FClasslikes%2F2039821458), opt_binaryMessageSerializeFn: (msg: [Message](../-message/index.md), writer: [BinaryWriter](../-binary-writer/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), opt_binaryMessageDeserializeFn: (msg: [Message](../-message/index.md), reader: [BinaryReader](../-binary-reader/index.md)) -&gt; [Message](../-message/index.md), opt_isPacked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [binaryReaderFn](binary-reader-fn.md) | [js]<br>open var [binaryReaderFn](binary-reader-fn.md): [BinaryRead](../index.md#-912733953%2FClasslikes%2F2039821458) |
| [binaryWriterFn](binary-writer-fn.md) | [js]<br>open var [binaryWriterFn](binary-writer-fn.md): [BinaryWrite](../index.md#-2100345842%2FClasslikes%2F2039821458) |
| [fieldInfo](field-info.md) | [js]<br>open var [fieldInfo](field-info.md): [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](index.md)&gt; |
| [opt_binaryMessageDeserializeFn](opt_binary-message-deserialize-fn.md) | [js]<br>open var [opt_binaryMessageDeserializeFn](opt_binary-message-deserialize-fn.md): (msg: [Message](../-message/index.md), reader: [BinaryReader](../-binary-reader/index.md)) -&gt; [Message](../-message/index.md) |
| [opt_binaryMessageSerializeFn](opt_binary-message-serialize-fn.md) | [js]<br>open var [opt_binaryMessageSerializeFn](opt_binary-message-serialize-fn.md): (msg: [Message](../-message/index.md), writer: [BinaryWriter](../-binary-writer/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [opt_isPacked](opt_is-packed.md) | [js]<br>open var [opt_isPacked](opt_is-packed.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
