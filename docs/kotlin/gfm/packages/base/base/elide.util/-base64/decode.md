//[base](../../../index.md)/[elide.util](../index.md)/[Base64](index.md)/[decode](decode.md)

# decode

[common, js, jvm]\
[common]\
expect open override fun [decode](decode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

[js, jvm]\
actual open override fun [decode](decode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Decode the provided [data](decode.md) from Base64, returning a raw set of bytes resulting from the decoding operation.

#### Return

Raw bytes of decoded data.

## Parameters

common

| | |
|---|---|
| data | Data to decode from Base64. |

js

| | |
|---|---|
| data | Data to decode from Base64. |

jvm

| | |
|---|---|
| data | Data to decode from Base64. |

native

| | |
|---|---|
| data | Raw bytes to decode from Base64. |

[native]\
actual open override fun [decode](decode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Decode the provided [data](decode.md) into a Base64-encoded set of bytes, which includes padding if necessary.

#### Return

Base64-decoded bytes.

## Parameters

common

| | |
|---|---|
| data | Data to decode from Base64. |

js

| | |
|---|---|
| data | Data to decode from Base64. |

jvm

| | |
|---|---|
| data | Data to decode from Base64. |

native

| | |
|---|---|
| data | Raw bytes to decode from Base64. |

[common, js, jvm, native]\
[common]\
expect open override fun [decode](decode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

[js, jvm, native]\
actual open override fun [decode](decode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Decode the provided [string](decode.md) from Base64, returning a raw set of bytes resulting from the decoding operation.

#### Return

Raw bytes of decoded data.

## Parameters

common

| | |
|---|---|
| string | String to decode from Base64. |

js

| | |
|---|---|
| string | String to decode from Base64. |

jvm

| | |
|---|---|
| string | String to decode from Base64. |

native

| | |
|---|---|
| string | String to decode from Base64. |
