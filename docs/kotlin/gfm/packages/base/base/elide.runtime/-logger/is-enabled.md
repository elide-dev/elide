//[base](../../../index.md)/[elide.runtime](../index.md)/[Logger](index.md)/[isEnabled](is-enabled.md)

# isEnabled

[common, js, jvm, native]\
[common]\
expect abstract fun [isEnabled](is-enabled.md)(level: [LogLevel](../-log-level/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

[js, native]\
actual abstract fun [isEnabled](is-enabled.md)(level: [LogLevel](../../../../../packages/base/base/elide.runtime/-log-level/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

[jvm]\
actual abstract fun [isEnabled](is-enabled.md)(level: [LogLevel](../-log-level/index.md#456488815%2FExtensions%2F-272498224)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Indicate whether the provided [level](is-enabled.md) is enabled for the current logger.

#### Return

Whether the log level is enabled.

## Parameters

common

| | |
|---|---|
| level | Log level to check. |

js

| | |
|---|---|
| level | Log level to check. |

jvm

| | |
|---|---|
| level | Log level to check. |

native

| | |
|---|---|
| level | Log level to check. |
