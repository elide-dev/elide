//[base](../../../index.md)/[elide.runtime](../index.md)/[LogLevel](index.md)

# LogLevel

[common]\
enum [LogLevel](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[LogLevel](index.md)&gt; 

Enumerates log levels on a given platform.

## Entries

| | |
|---|---|
| [TRACE](-t-r-a-c-e/index.md) | [common]<br>[TRACE](-t-r-a-c-e/index.md) |
| [DEBUG](-d-e-b-u-g/index.md) | [common]<br>[DEBUG](-d-e-b-u-g/index.md) |
| [WARN](-w-a-r-n/index.md) | [common]<br>[WARN](-w-a-r-n/index.md) |
| [INFO](-i-n-f-o/index.md) | [common]<br>[INFO](-i-n-f-o/index.md) |
| [ERROR](-e-r-r-o-r/index.md) | [common]<br>[ERROR](-e-r-r-o-r/index.md) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [common]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [LogLevel](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [common]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[LogLevel](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

## Properties

| Name | Summary |
|---|---|
| [name](../../elide.util/-encoding/-b-a-s-e64/index.md#-372974862%2FProperties%2F-1416663450) | [common]<br>val [name](../../elide.util/-encoding/-b-a-s-e64/index.md#-372974862%2FProperties%2F-1416663450): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ordinal](../../elide.util/-encoding/-b-a-s-e64/index.md#-739389684%2FProperties%2F-1416663450) | [common]<br>val [ordinal](../../elide.util/-encoding/-b-a-s-e64/index.md#-739389684%2FProperties%2F-1416663450): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

## Extensions

| Name | Summary |
|---|---|
| [isEnabled](../../elide.runtime.jvm/is-enabled.md) | [jvm]<br>fun [LogLevel](index.md#456488815%2FExtensions%2F-272498224).[isEnabled](../../elide.runtime.jvm/is-enabled.md)(logger: Logger): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [jvmLevel](../../elide.runtime.jvm/jvm-level.md) | [jvm]<br>val [LogLevel](index.md#456488815%2FExtensions%2F-272498224).[jvmLevel](../../elide.runtime.jvm/jvm-level.md): Level |
| [resolve](../../elide.runtime.jvm/resolve.md) | [jvm]<br>fun [LogLevel](index.md#456488815%2FExtensions%2F-272498224).[resolve](../../elide.runtime.jvm/resolve.md)(logger: Logger): ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
