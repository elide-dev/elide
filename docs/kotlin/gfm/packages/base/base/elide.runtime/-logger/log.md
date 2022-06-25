//[base](../../../index.md)/[elide.runtime](../index.md)/[Logger](index.md)/[log](log.md)

# log

[common, js, jvm, native]\
[common]\
expect abstract fun [log](log.md)(level: [LogLevel](../-log-level/index.md), message: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, levelChecked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)

[js, jvm, native]\
actual abstract fun [log](log.md)(level: [LogLevel](../-log-level/index.md), message: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, levelChecked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))

Log one or more arbitrary [message](log.md)s to the console or log, depending on the current platform.

Each argument will be converted to a string, and all strings will be concatenated together into one log message. To engage in string formatting, or callable log messages, see other variants of this same method.

## Parameters

common

| | |
|---|---|
| level | Level that this log is being logged at. |
| message | Set of messages to log in this entry. |
| levelChecked | Whether the log level has already been checked. |

js

| | |
|---|---|
| level | Level that this log is being logged at. |
| message | Set of messages to log in this entry. |
| levelChecked | Whether the log level has already been checked. |

jvm

| | |
|---|---|
| level | Level that this log is being logged at. |
| message | Set of messages to log in this entry. |
| levelChecked | Whether the log level has already been checked. |

native

| | |
|---|---|
| level | Level that this log is being logged at. |
| message | Set of messages to log in this entry. |
| levelChecked | Whether the log level has already been checked. |
