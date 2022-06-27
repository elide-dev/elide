//[base](../../../index.md)/[lib.protobuf](../index.md)/[Message](index.md)

# Message

[js]\
open external class [Message](index.md)

## Constructors

| | |
|---|---|
| [Message](-message.md) | [js]<br>fun [Message](-message.md)() |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [js]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [clone](clone.md) | [js]<br>open fun [clone](clone.md)(): [Message](index.md) |
| [cloneMessage](clone-message.md) | [js]<br>open fun [cloneMessage](clone-message.md)(): [Message](index.md) |
| [getExtension](get-extension.md) | [js]<br>open fun &lt;[T](get-extension.md)&gt; [getExtension](get-extension.md)(fieldInfo: [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](get-extension.md)&gt;): [T](get-extension.md) |
| [getJsPbMessageId](get-js-pb-message-id.md) | [js]<br>open fun [getJsPbMessageId](get-js-pb-message-id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [readBinaryExtension](read-binary-extension.md) | [js]<br>open fun &lt;[T](read-binary-extension.md)&gt; [readBinaryExtension](read-binary-extension.md)(proto: [Message](index.md), reader: [BinaryReader](../-binary-reader/index.md), extensions: [T$1](../-t$1/index.md), setExtensionFn: (fieldInfo: [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](read-binary-extension.md)&gt;, param_val: [T](read-binary-extension.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [serializeBinary](serialize-binary.md) | [js]<br>open fun [serializeBinary](serialize-binary.md)(): [Uint8Array](https://kotlinlang.org/api/latest/jvm/stdlib/org.khronos.webgl/-uint8-array/index.html) |
| [serializeBinaryExtensions](serialize-binary-extensions.md) | [js]<br>open fun &lt;[T](serialize-binary-extensions.md)&gt; [serializeBinaryExtensions](serialize-binary-extensions.md)(proto: [Message](index.md), writer: [BinaryWriter](../-binary-writer/index.md), extensions: [T$1](../-t$1/index.md), getExtensionFn: (fieldInfo: [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](serialize-binary-extensions.md)&gt;) -&gt; [T](serialize-binary-extensions.md)) |
| [setExtension](set-extension.md) | [js]<br>open fun &lt;[T](set-extension.md)&gt; [setExtension](set-extension.md)(fieldInfo: [ExtensionFieldInfo](../-extension-field-info/index.md)&lt;[T](set-extension.md)&gt;, value: [T](set-extension.md)) |
| [toArray](to-array.md) | [js]<br>open fun [toArray](to-array.md)(): [MessageArray](../index.md#-185924924%2FClasslikes%2F-431612152) |
| [toObject](to-object.md) | [js]<br>open fun [toObject](to-object.md)(includeInstance: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = definedExternally): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [toString](to-string.md) | [js]<br>open override fun [toString](to-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
