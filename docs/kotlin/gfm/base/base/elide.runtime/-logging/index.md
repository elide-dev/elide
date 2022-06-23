//[base](../../../index.md)/[elide.runtime](../index.md)/[Logging](index.md)

# Logging

[common]\
expect class [Logging](index.md)

Describes an expected class which is able to produce [Logger](../-logger/index.md) instances as a factory.

[js, jvm, native]\
actual class [Logging](index.md)

Describes an expected class which is able to produce [Logger](../-logger/index.md) instances as a factory.

## Constructors

| | |
|---|---|
| [Logging](-logging.md) | [js, native]<br>fun [Logging](-logging.md)() |

## Types

| Name | Summary |
|---|---|
| [Companion](../../../../base/base/elide.runtime/-logging/[jvm]-companion/index.md) | [js, jvm]<br>[js]<br>object [Companion]([js]-companion/index.md)<br>[jvm]<br>object [Companion]([jvm]-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [logger](logger.md) | [common, js, jvm, native]<br>[common]<br>expect fun [logger](logger.md)(): [Logger](../-logger/index.md)<br>[js, jvm, native]<br>actual fun [logger](logger.md)(): [Logger](../-logger/index.md)<br>Acquire a root [Logger](../-logger/index.md) which is unnamed, or uses an empty string value (`""`) for the logger name.<br>[common, js, jvm, native]<br>[common]<br>expect fun [logger](logger.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Logger](../-logger/index.md)<br>[js, jvm, native]<br>actual fun [logger](logger.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Logger](../-logger/index.md)<br>Acquire a [Logger](../-logger/index.md) for the given logger [name](logger.md), which can be any identifying string; in JVM circumstances, the full class name of the subject which is sending the logs is usually used. |
