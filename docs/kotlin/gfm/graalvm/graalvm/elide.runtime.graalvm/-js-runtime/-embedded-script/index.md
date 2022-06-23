//[graalvm](../../../../index.md)/[elide.runtime.graalvm](../../index.md)/[JsRuntime](../index.md)/[EmbeddedScript](index.md)

# EmbeddedScript

[jvm]\
class [EmbeddedScript](index.md)(val path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), charset: [Charset](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html) = StandardCharsets.UTF_8, val invocationBase: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val invocationTarget: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) : [JsRuntime.ExecutableScript](../-executable-script/index.md)

Embedded script implementation which pulls from local JAR resources.

## Constructors

| | |
|---|---|
| [EmbeddedScript](-embedded-script.md) | [jvm]<br>fun [EmbeddedScript](-embedded-script.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), charset: [Charset](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html) = StandardCharsets.UTF_8, invocationBase: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, invocationTarget: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) |

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
| [path](path.md) | [jvm]<br>val [path](path.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
