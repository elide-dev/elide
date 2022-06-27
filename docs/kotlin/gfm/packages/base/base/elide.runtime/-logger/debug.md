//[base](../../../index.md)/[elide.runtime](../index.md)/[Logger](index.md)/[debug](debug.md)

# debug

[common, js, jvm, native]\
[common]\
expect open fun [debug](debug.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

[js, jvm, native]\
actual open fun [debug](debug.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

Log one or more arbitrary [message](debug.md)s to the console or log, at the level of [LogLevel.DEBUG](../-log-level/-d-e-b-u-g/index.md).

Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log](log.md). To engage in string formatting, or callable log messages, see other variants of this same method.

## See also

common

| | |
|---|---|
| [elide.runtime.Logger](info.md) | other variants of this method. |

js

| | |
|---|---|
| [elide.runtime.Logger](info.md) | other variants of this method. |

jvm

| | |
|---|---|
| [elide.runtime.Logger](info.md) | other variants of this method. |

native

| | |
|---|---|
| [elide.runtime.Logger](info.md) | other variants of this method. |

## Parameters

common

| | |
|---|---|
| message | Set of messages to log in this entry. |

js

| | |
|---|---|
| message | Set of messages to log in this entry. |

jvm

| | |
|---|---|
| message | Set of messages to log in this entry. |

native

| | |
|---|---|
| message | Set of messages to log in this entry. |

[common, js, jvm, native]\
[common]\
expect open fun [debug](debug.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

[js, jvm, native]\
actual open fun [debug](debug.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Log the message produced by the provided [producer](debug.md), at the level of [LogLevel.DEBUG](../-log-level/-d-e-b-u-g/index.md), assuming debug-level logging is currently enabled.

If debug logging is not active, the producer will not be dispatched.

## Parameters

common

| | |
|---|---|
| producer | Function that produces the message to log. |

js

| | |
|---|---|
| producer | Function that produces the message to log. |

jvm

| | |
|---|---|
| producer | Function that produces the message to log. |

native

| | |
|---|---|
| producer | Function that produces the message to log. |
