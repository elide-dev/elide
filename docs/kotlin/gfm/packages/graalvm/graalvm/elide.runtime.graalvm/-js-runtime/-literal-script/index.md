//[graalvm](../../../../index.md)/[elide.runtime.graalvm](../../index.md)/[JsRuntime](../index.md)/[LiteralScript](index.md)

# LiteralScript

[jvm]\
class [LiteralScript](index.md)(moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), script: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [JsRuntime.ExecutableScript](../-executable-script/index.md)

Embedded script implementation which pulls from a string literal.

## Constructors

| | |
|---|---|
| [LiteralScript](-literal-script.md) | [jvm]<br>fun [LiteralScript](-literal-script.md)(moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), script: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [getId](get-id.md) | [jvm]<br>open override fun [getId](get-id.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [load](load.md) | [jvm]<br>open override fun [load](load.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [invocationBase](../-executable-script/invocation-base.md) | [jvm]<br>val [invocationBase](../-executable-script/invocation-base.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [invocationTarget](../-executable-script/invocation-target.md) | [jvm]<br>val [invocationTarget](../-executable-script/invocation-target.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
