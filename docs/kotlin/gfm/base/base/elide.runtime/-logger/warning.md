//[base](../../../index.md)/[elide.runtime](../index.md)/[Logger](index.md)/[warning](warning.md)

# warning

[common, js, jvm, native]\
[common]\
expect open fun [warning](warning.md)(vararg message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

[js, jvm, native]\
actual open fun [warning](warning.md)(vararg message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Log one or more arbitrary [message](warning.md)s to the console or log, at the level of [LogLevel.WARN](../-log-level/-w-a-r-n/index.md).

Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log](log.md). To engage in string formatting, or callable log messages, see other variants of this same method.

This method is a thin alias for the equivalent [warn](warn.md) call.

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
expect open fun [warning](warning.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), vararg context: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

[js, jvm, native]\
actual open fun [warning](warning.md)(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), vararg context: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

Log the provided [message](warning.md) to the console at the [LogLevel.WARN](../-log-level/-w-a-r-n/index.md) level along with any provided [context](warning.md) values, each of which will be string formatted before being concatenated and logged or printed.

This method is a thin alias for the equivalent [warn](warn.md) call.

## Parameters

common

| | |
|---|---|
| message | Message to emit to the log as a prefix for [context](warning.md) values. |
| context | Additional context values, potentially strings, to string-format and concatenate. |

js

| | |
|---|---|
| message | lib.protobuf.Message to emit to the log as a prefix for [context](warning.md) values. |
| context | Additional context values, potentially strings, to string-format and concatenate. |

jvm

| | |
|---|---|
| message | Message to emit to the log as a prefix for [context](warning.md) values. |
| context | Additional context values, potentially strings, to string-format and concatenate. |

native

| | |
|---|---|
| message | Message to emit to the log as a prefix for [context](warning.md) values. |
| context | Additional context values, potentially strings, to string-format and concatenate. |

[common, js, jvm, native]\
[common]\
expect open fun [warning](warning.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

[js, jvm, native]\
actual open fun [warning](warning.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Log the message produced by the provided [producer](warning.md), at the level of [LogLevel.WARN](../-log-level/-w-a-r-n/index.md), assuming warn-level logging is currently enabled.

If warn logging is not active, the producer will not be dispatched. This method is a thin alias for the equivalent [warn](warn.md) call.

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
