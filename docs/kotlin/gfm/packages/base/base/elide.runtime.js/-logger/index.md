//[base](../../../index.md)/[elide.runtime.js](../index.md)/[Logger](index.md)

# Logger

[js]\
data class [Logger](index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) : [Logger](../../elide.runtime/-logger/index.md)

Specifies a lightweight [elide.runtime.Logger](../../elide.runtime/-logger/index.md) implementation for use in JavaScript.

## Constructors

| | |
|---|---|
| [Logger](-logger.md) | [js]<br>fun [Logger](-logger.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |

## Functions

| Name | Summary |
|---|---|
| [debug](../../elide.runtime/-logger/debug.md) | [js]<br>actual open fun [debug](../../elide.runtime/-logger/debug.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>Log one or more arbitrary [message](../../elide.runtime/-logger/debug.md)s to the console or log, at the level of [LogLevel.DEBUG](../../elide.runtime/-log-level/-d-e-b-u-g/index.md).<br>[js]<br>actual open fun [debug](../../elide.runtime/-logger/debug.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Log the message produced by the provided [producer](../../elide.runtime/-logger/debug.md), at the level of [LogLevel.DEBUG](../../elide.runtime/-log-level/-d-e-b-u-g/index.md), assuming debug-level logging is currently enabled. |
| [error](../../elide.runtime/-logger/error.md) | [js]<br>actual open fun [error](../../elide.runtime/-logger/error.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>Log one or more arbitrary [message](../../elide.runtime/-logger/error.md)s to the console or log, at the level of [LogLevel.ERROR](../../elide.runtime/-log-level/-e-r-r-o-r/index.md).<br>[js]<br>actual open fun [error](../../elide.runtime/-logger/error.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Log the message produced by the provided [producer](../../elide.runtime/-logger/error.md), at the level of [LogLevel.ERROR](../../elide.runtime/-log-level/-e-r-r-o-r/index.md), assuming error-level logging is currently enabled. |
| [info](../../elide.runtime/-logger/info.md) | [js]<br>actual open fun [info](../../elide.runtime/-logger/info.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>Log one or more arbitrary [message](../../elide.runtime/-logger/info.md)s to the console or log, at the level of [LogLevel.INFO](../../elide.runtime/-log-level/-i-n-f-o/index.md).<br>[js]<br>actual open fun [info](../../elide.runtime/-logger/info.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Log the message produced by the provided [producer](../../elide.runtime/-logger/info.md), at the level of [LogLevel.INFO](../../elide.runtime/-log-level/-i-n-f-o/index.md), assuming info-level logging is currently enabled. |
| [isEnabled](is-enabled.md) | [js]<br>open override fun [isEnabled](is-enabled.md)(level: [LogLevel](../../elide.runtime/-log-level/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [log](log.md) | [js]<br>open override fun [log](log.md)(level: [LogLevel](../../elide.runtime/-log-level/index.md), message: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, levelChecked: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)) |
| [trace](../../elide.runtime/-logger/trace.md) | [js]<br>actual open fun [trace](../../elide.runtime/-logger/trace.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>Log one or more arbitrary [message](../../elide.runtime/-logger/trace.md)s to the console or log, at the level of [LogLevel.TRACE](../../elide.runtime/-log-level/-t-r-a-c-e/index.md).<br>[js]<br>actual open fun [trace](../../elide.runtime/-logger/trace.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Log the message produced by the provided [producer](../../elide.runtime/-logger/trace.md), at the level of [LogLevel.TRACE](../../elide.runtime/-log-level/-t-r-a-c-e/index.md), assuming trace-level logging is currently enabled. |
| [warn](../../elide.runtime/-logger/warn.md) | [js]<br>actual open fun [warn](../../elide.runtime/-logger/warn.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>Log one or more arbitrary [message](../../elide.runtime/-logger/warn.md)s to the console or log, at the level of [LogLevel.WARN](../../elide.runtime/-log-level/-w-a-r-n/index.md).<br>[js]<br>actual open fun [warn](../../elide.runtime/-logger/warn.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Log the message produced by the provided [producer](../../elide.runtime/-logger/warn.md), at the level of [LogLevel.WARN](../../elide.runtime/-log-level/-w-a-r-n/index.md), assuming warn-level logging is currently enabled. |
| [warning](../../elide.runtime/-logger/warning.md) | [js]<br>actual open fun [warning](../../elide.runtime/-logger/warning.md)(vararg message: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))<br>Log one or more arbitrary [message](../../elide.runtime/-logger/warning.md)s to the console or log, at the level of [LogLevel.WARN](../../elide.runtime/-log-level/-w-a-r-n/index.md).<br>[js]<br>actual open fun [warning](../../elide.runtime/-logger/warning.md)(producer: () -&gt; [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Log the message produced by the provided [producer](../../elide.runtime/-logger/warning.md), at the level of [LogLevel.WARN](../../elide.runtime/-log-level/-w-a-r-n/index.md), assuming warn-level logging is currently enabled. |

## Properties

| Name | Summary |
|---|---|
| [name](name.md) | [js]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
