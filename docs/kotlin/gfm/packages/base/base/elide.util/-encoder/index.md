//[base](../../../index.md)/[elide.util](../index.md)/[Encoder](index.md)

# Encoder

[common]\
interface [Encoder](index.md)

Specifies the expected API interface for an encoding tool, which is capable of encoding data to a given format or expression, as well as decoding from that same format.

## Functions

| Name | Summary |
|---|---|
| [decode](decode.md) | [common]<br>abstract fun [decode](decode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>abstract fun [decode](decode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) |
| [decodeToString](decode-to-string.md) | [common]<br>abstract fun [decodeToString](decode-to-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>abstract fun [decodeToString](decode-to-string.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [encode](encode.md) | [common]<br>abstract fun [encode](encode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>abstract fun [encode](encode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) |
| [encodeToString](encode-to-string.md) | [common]<br>abstract fun [encodeToString](encode-to-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>abstract fun [encodeToString](encode-to-string.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [encoding](encoding.md) | [common]<br>abstract fun [encoding](encoding.md)(): [Encoding](../-encoding/index.md)<br>Return the enumerated [Encoding](../-encoding/index.md) which is implemented by this [Encoder](index.md). |

## Inheritors

| Name |
|---|
| [Hex](../-hex/index.md) |
| [Base64](../-base64/index.md) |
