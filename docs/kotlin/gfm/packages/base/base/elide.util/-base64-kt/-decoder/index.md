//[base](../../../../index.md)/[elide.util](../../index.md)/[Base64Kt](../index.md)/[Decoder](index.md)

# Decoder

[common]\
class [Decoder](index.md)

This class implements a decoder for decoding byte data using the Base64 encoding scheme as specified in RFC 4648 and RFC 2045.

The Base64 padding character `'='` is accepted and interpreted as the end of the encoded byte data, but is not required. So if the final unit of the encoded byte data only has two or three Base64 characters (without the corresponding padding character(s) padded), they are decoded as if followed by padding character(s). If there is a padding character present in the final unit, the correct number of padding character(s) must be present, otherwise `IllegalArgumentException` ( `IOException` when reading from a Base64 stream) is thrown during decoding.

Instances of [Decoder](index.md) class are safe for use by multiple concurrent threads.

Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to be thrown.

#### Since

1.8

## See also

common

| | |
|---|---|
| [elide.util.Base64Kt.Encoder](../-encoder/index.md) |  |

## Constructors

| | |
|---|---|
| [Decoder](-decoder.md) | [common]<br>fun [Decoder](-decoder.md)() |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [common]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [decode](decode.md) | [common]<br>fun [decode](decode.md)(src: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)<br>Decodes all bytes from the input byte array using the [Base64](../../-base64/index.md) encoding scheme, writing the results into a newly-allocated output byte array. The returned byte array is of the length of the resulting bytes. |
