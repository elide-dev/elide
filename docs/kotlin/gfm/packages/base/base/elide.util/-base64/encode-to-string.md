//[base](../../../index.md)/[elide.util](../index.md)/[Base64](index.md)/[encodeToString](encode-to-string.md)

# encodeToString

[common, js, jvm, native]\
[common]\
expect open override fun [encodeToString](encode-to-string.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

[js, jvm, native]\
actual open override fun [encodeToString](encode-to-string.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Encode the provided [string](encode-to-string.md) into a Base64-encoded string, which includes padding if necessary.

#### Return

Base64-encoded string.

#### Parameters

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
expect open override fun [encodeToString](encode-to-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

[js, jvm, native]\
actual open override fun [encodeToString](encode-to-string.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Encode the provided [data](encode-to-string.md) into a Base64-encoded string, which includes padding if necessary.

#### Return

Base64-encoded string.

#### Parameters

common

| | |
|---|---|
| data | Raw bytes to encode into a Base64 string. |

js

| | |
|---|---|
| data | Raw bytes to encode into a Base64 string. |

jvm

| | |
|---|---|
| data | Raw bytes to encode into a Base64 string. |

native

| | |
|---|---|
| data | Raw bytes to encode with Base64. |
