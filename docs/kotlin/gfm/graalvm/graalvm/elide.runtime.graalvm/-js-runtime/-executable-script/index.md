//[graalvm](../../../../index.md)/[elide.runtime.graalvm](../../index.md)/[JsRuntime](../index.md)/[ExecutableScript](index.md)

# ExecutableScript

[jvm]\
sealed class [ExecutableScript](index.md)

Embedded script descriptor object.

## Functions

| Name | Summary |
|---|---|
| [getId](get-id.md) | [jvm]<br>abstract fun [getId](get-id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [load](load.md) | [jvm]<br>abstract fun [load](load.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [invocationBase](invocation-base.md) | [jvm]<br>val [invocationBase](invocation-base.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [invocationTarget](invocation-target.md) | [jvm]<br>val [invocationTarget](invocation-target.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |

## Inheritors

| Name |
|---|
| [EmbeddedScript](../-embedded-script/index.md) |
| [LiteralScript](../-literal-script/index.md) |
