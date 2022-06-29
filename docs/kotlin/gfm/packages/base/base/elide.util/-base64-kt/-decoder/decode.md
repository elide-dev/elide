//[base](../../../../index.md)/[elide.util](../../index.md)/[Base64Kt](../index.md)/[Decoder](index.md)/[decode](decode.md)

# decode

[common]\
fun [decode](decode.md)(src: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Decodes all bytes from the input byte array using the [Base64](../../-base64/index.md) encoding scheme, writing the results into a newly-allocated output byte array. The returned byte array is of the length of the resulting bytes.

#### Return

A newly-allocated byte array containing the decoded bytes.

## Parameters

common

| | |
|---|---|
| src | the byte array to decode |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | if `src` is not in valid Base64 scheme |
