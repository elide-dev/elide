//[base](../../index.md)/[elide.util](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [Base64](-base64/index.md) | [common, js, jvm, native]<br>[common]<br>expect object [Base64](-base64/index.md) : [Encoder](-encoder/index.md)<br>[js, jvm, native]<br>actual object [Base64](-base64/index.md) : [Encoder](../../../../packages/base/base/elide.util/-encoder/index.md)<br>Cross-platform utilities for encoding and decoding to/from Base64. |
| [Base64Kt](-base64-kt/index.md) | [common]<br>object [Base64Kt](-base64-kt/index.md)<br>This class consists exclusively of static methods for obtaining encoders and decoders for the Base64 encoding scheme. The implementation of this class supports the following types of Base64 as specified in [RFC 4648](http://www.ietf.org/rfc/rfc4648.txt) and [RFC 2045](http://www.ietf.org/rfc/rfc2045.txt). |
| [Encoder](-encoder/index.md) | [common]<br>interface [Encoder](-encoder/index.md)<br>Specifies the expected API interface for an encoding tool, which is capable of encoding data to a given format or expression, as well as decoding from that same format. |
| [Encoding](-encoding/index.md) | [common]<br>enum [Encoding](-encoding/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[Encoding](-encoding/index.md)&gt; <br>Enumerates supported encodings and binds [Encoder](-encoder/index.md) instances to each. |
| [Hex](-hex/index.md) | [common]<br>object [Hex](-hex/index.md) : [Encoder](-encoder/index.md)<br>Provides cross-platform utilities for encoding values into hex, or decoding values from hex. |
| [UUID](-u-u-i-d/index.md) | [common, js, jvm, native]<br>[common]<br>expect object [UUID](-u-u-i-d/index.md)<br>[js, jvm, native]<br>actual object [UUID](-u-u-i-d/index.md)<br>UUID tools provided to all platforms. |

## Functions

| Name | Summary |
|---|---|
| [toBase64](to-base64.md) | [common]<br>fun [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html).[toBase64](to-base64.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode the current [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) to a Base64 byte array, using the cross-platform [Base64](-base64/index.md) tools.<br>[common]<br>fun [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html).[toBase64](to-base64.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode the current [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a Base64 byte array, using the cross-platform [Base64](-base64/index.md) tools. |
| [toBase64String](to-base64-string.md) | [common]<br>fun [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html).[toBase64String](to-base64-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Encode the current [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) to a Base64 string, using the cross-platform [Base64](-base64/index.md) tools.<br>[common]<br>fun [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html).[toBase64String](to-base64-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Encode the current [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a Base64 string, using the cross-platform [Base64](-base64/index.md) tools. |
| [toHex](to-hex.md) | [common]<br>fun [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html).[toHex](to-hex.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encode the current [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a hex-encoded byte array, using the cross-platform [Hex](-hex/index.md) tools. |
| [toHexString](to-hex-string.md) | [common]<br>fun [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html).[toHexString](to-hex-string.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Encode the current [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) to a hex-encoded string, using the cross-platform [Hex](-hex/index.md) tools. |
