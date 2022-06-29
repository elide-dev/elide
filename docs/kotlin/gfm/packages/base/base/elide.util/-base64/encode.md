//[base](../../../index.md)/[elide.util](../index.md)/[Base64](index.md)/[encode](encode.md)

# encode

[common]\
expect open override fun [encode](encode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Encode the provided [string](encode.md) into a Base64-encoded byte array, which includes padding if necessary.

#### Return

Base64-encoded bytes.

## Parameters

common

| | |
|---|---|
| string | String to encode with Base64. |

js

| | |
|---|---|
| string | String to encode with Base64. |

jvm

| | |
|---|---|
| string | String to encode with Base64. |

native

| | |
|---|---|
| string | String to encode with Base64. |

[js, jvm, native]\
[js, jvm, native]\
actual open override fun [encode](encode.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Encode the provided [string](encode.md) into a Base64-encoded string, which includes padding if necessary.

#### Return

Base64-encoded string.

## Parameters

common

| | |
|---|---|
| string | String to encode with Base64. |

js

| | |
|---|---|
| string | String to encode with Base64. |

jvm

| | |
|---|---|
| string | String to encode with Base64. |

native

| | |
|---|---|
| string | String to encode with Base64. |

[common, js, jvm, native]\
[common]\
expect open override fun [encode](encode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

[js, jvm, native]\
actual open override fun [encode](encode.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Encode the provided [data](encode.md) into a Base64-encoded set of bytes, which includes padding if necessary.

#### Return

Base64-encoded bytes.

## Parameters

common

| | |
|---|---|
| data | Raw bytes to encode with Base64. |

js

| | |
|---|---|
| data | Raw bytes to encode with Base64. |

jvm

| | |
|---|---|
| data | Raw bytes to encode with Base64. |

native

| | |
|---|---|
| data | Raw bytes to encode with Base64. |
