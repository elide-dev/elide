//[base](../../../index.md)/[elide.runtime](../index.md)/[Logger](index.md)/[warn](warn.md)

# warn

[common]\
expect open fun [warn](warn.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

Log one or more arbitrary [message](warn.md)s to the console or log, at the level of [LogLevel.WARN](../-log-level/-w-a-r-n/index.md).

Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log](log.md). To engage in string formatting, or callable log messages, see other variants of this same method.

#### See also

common

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

js

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

jvm

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

native

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

#### Parameters

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

[js, jvm, native]\
[js, jvm, native]\
actual open fun [warn](warn.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

Log one or more arbitrary [message](warn.md)s to the console or log, at the level of [LogLevel.WARN](../../../../../packages/base/base/elide.runtime/-log-level/-w-a-r-n/index.md).

Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log](log.md). To engage in string formatting, or callable log messages, see other variants of this same method.

#### See also

common

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

js

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

jvm

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

native

| | |
|---|---|
| [Logger.info](info.md) | other variants of this method. |

#### Parameters

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

[common]\
expect open fun [warn](warn.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Log the message produced by the provided [producer](warn.md), at the level of [LogLevel.WARN](../-log-level/-w-a-r-n/index.md), assuming warn-level logging is currently enabled.

If warn logging is not active, the producer will not be dispatched.

#### Parameters

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

[js, jvm, native]\
[js, jvm, native]\
actual open fun [warn](warn.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))

Log the message produced by the provided [producer](warn.md), at the level of [LogLevel.WARN](../../../../../packages/base/base/elide.runtime/-log-level/-w-a-r-n/index.md), assuming warn-level logging is currently enabled.

If warn logging is not active, the producer will not be dispatched.

#### Parameters

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
