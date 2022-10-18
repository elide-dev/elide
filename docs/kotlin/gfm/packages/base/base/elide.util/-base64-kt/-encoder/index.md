//[base](../../../../index.md)/[elide.util](../../index.md)/[Base64Kt](../index.md)/[Encoder](index.md)

# Encoder

[common]\
class [Encoder](index.md)

This class implements an encoder for encoding byte data using the Base64 encoding scheme as specified in RFC 4648 and RFC 2045.

Instances of [Encoder](index.md) class are safe for use by multiple concurrent threads.

Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to be thrown.

#### Since

1.8

#### See also

common

| |
|---|
| [Base64Kt.Decoder](../-decoder/index.md) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [encode](encode.md) | [common]<br>fun [encode](encode.md)(src: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Encodes all bytes from the specified byte array into a newly-allocated byte array using the [Base64](../../-base64/index.md) encoding scheme. The returned byte array is of the length of the resulting bytes. |
