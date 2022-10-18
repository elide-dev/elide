//[base](../../../index.md)/[elide.util](../index.md)/[Base64](index.md)/[encodeWebSafe](encode-web-safe.md)

# encodeWebSafe

[common, js, jvm, native]\
[common]\
expect fun [encodeWebSafe](encode-web-safe.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

[js, jvm, native]\
actual fun [encodeWebSafe](encode-web-safe.md)(string: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Encode the provided [string](encode-web-safe.md) into a Base64-encoded string, omitting characters which are unsafe for use on the web, including padding characters, which are not emitted.

#### Return

Base64-encoded string, using only web-safe characters.

#### Parameters

common

| | |
|---|---|
| string | String to encode with web-safe Base64. |

js

| | |
|---|---|
| string | String to encode with web-safe Base64. |

jvm

| | |
|---|---|
| string | String to encode with web-safe Base64. |

native

| | |
|---|---|
| string | String to encode with web-safe Base64. |

[common, js, jvm, native]\
[common]\
expect fun [encodeWebSafe](encode-web-safe.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

[js, jvm, native]\
actual fun [encodeWebSafe](encode-web-safe.md)(data: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Encode the provided [data](encode-web-safe.md) into a Base64-encoded set of bytes, omitting characters which are unsafe for use on the web, including padding characters, which are not emitted.

#### Return

Base64-encoded bytes, using only web-safe characters.

#### Parameters

common

| | |
|---|---|
| data | Raw bytes to encode with web-safe Base64. |

js

| | |
|---|---|
| data | Raw bytes to encode with web-safe Base64. |

jvm

| | |
|---|---|
| data | Raw bytes to encode with web-safe Base64. |

native

| | |
|---|---|
| data | Raw bytes to encode with web-safe Base64. |
