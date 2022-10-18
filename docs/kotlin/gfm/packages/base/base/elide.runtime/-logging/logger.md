//[base](../../../index.md)/[elide.runtime](../index.md)/[Logging](index.md)/[logger](logger.md)

# logger

[common, js, jvm, native]\
[common]\
expect fun [logger](logger.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Logger](../-logger/index.md)

[js, jvm, native]\
actual fun [logger](logger.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [Logger](../-logger/index.md)

Acquire a [Logger](../-logger/index.md) for the given logger [name](logger.md), which can be any identifying string; in JVM circumstances, the full class name of the subject which is sending the logs is usually used.

#### Return

Desired logger.

#### Parameters

common

| | |
|---|---|
| name | Name of the logger to create and return. |

js

| | |
|---|---|
| name | Name of the logger to create and return. |

jvm

| | |
|---|---|
| name | Name of the logger to create and return. |

native

| | |
|---|---|
| name | Name of the logger to create and return. |

[common, js, jvm, native]\
[common]\
expect fun [logger](logger.md)(): [Logger](../-logger/index.md)

[js, jvm, native]\
actual fun [logger](logger.md)(): [Logger](../-logger/index.md)

Acquire a root [Logger](../-logger/index.md) which is unnamed, or uses an empty string value (`""`) for the logger name.

#### Return

Root logger.
