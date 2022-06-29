//[base](../../../index.md)/[elide.util](../index.md)/[Hex](index.md)

# Hex

[common]\
object [Hex](index.md) : [Encoder](../-encoder/index.md)

Provides cross-platform utilities for encoding values into hex, or decoding values from hex.

## Functions

| Name | Summary |
|---|---|
| [decode](decode.md) | [common]<br>open override fun [decode](decode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Decode the provided [data](decode.md) as a byte array of hex-encoded data.<br>[common]<br>open override fun [decode](decode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Decode the provided [string](decode.md) as a byte array of hex-encoded data. |
| [decodeToString](decode-to-string.md) | [common]<br>open override fun [decodeToString](decode-to-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Decode the provided [data](decode-to-string.md) into a string of decoded data.<br>[common]<br>open override fun [decodeToString](decode-to-string.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Decode the provided [string](decode-to-string.md) into a string of decoded data. |
| [encode](encode.md) | [common]<br>open override fun [encode](encode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode the provided [data](encode.md) as a byte array of hex-encoded data.<br>[common]<br>open override fun [encode](encode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode the provided [string](encode.md) as a byte array of hex-encoded data. |
| [encodeToString](encode-to-string.md) | [common]<br>open override fun [encodeToString](encode-to-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Encode the provided [data](encode-to-string.md) as a string of hex-encoded data.<br>[common]<br>open override fun [encodeToString](encode-to-string.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Encode the provided [string](encode-to-string.md) as a string of hex-encoded data. |
| [encoding](encoding.md) | [common]<br>open override fun [encoding](encoding.md)(): [Encoding](../-encoding/index.md) |

## Properties

| Name | Summary |
|---|---|
| [CHARACTER_SET](-c-h-a-r-a-c-t-e-r_-s-e-t.md) | [common]<br>val [CHARACTER_SET](-c-h-a-r-a-c-t-e-r_-s-e-t.md): [CharArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char-array/index.html)<br>Array of hex-allowable characters. |
