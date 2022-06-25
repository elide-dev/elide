//[graalvm](../../../index.md)/[elide.runtime.graalvm](../index.md)/[JsRuntimeConfig](index.md)

# JsRuntimeConfig

[jvm]\
data class [JsRuntimeConfig](index.md)(val entry: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val artifacts: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[JsRuntimeConfig.JsRuntimeArtifact](-js-runtime-artifact/index.md)&gt;)

Runtime configuration for the GraalVM JavaScript engine.

## Parameters

jvm

| | |
|---|---|
| entry | Entrypoint file which should cap the rendered script. |
| artifacts | Artifacts to load for runtime use. |

## Constructors

| | |
|---|---|
| [JsRuntimeConfig](-js-runtime-config.md) | [jvm]<br>fun [JsRuntimeConfig](-js-runtime-config.md)(entry: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), artifacts: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[JsRuntimeConfig.JsRuntimeArtifact](-js-runtime-artifact/index.md)&gt;) |

## Types

| Name | Summary |
|---|---|
| [JsRuntimeArtifact](-js-runtime-artifact/index.md) | [jvm]<br>data class [JsRuntimeArtifact](-js-runtime-artifact/index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>Defines a single artifact which is loaded for runtime use. |

## Properties

| Name | Summary |
|---|---|
| [artifacts](artifacts.md) | [jvm]<br>val [artifacts](artifacts.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[JsRuntimeConfig.JsRuntimeArtifact](-js-runtime-artifact/index.md)&gt; |
| [entry](entry.md) | [jvm]<br>val [entry](entry.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
