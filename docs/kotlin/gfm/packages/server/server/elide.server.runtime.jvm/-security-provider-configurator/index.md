//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[SecurityProviderConfigurator](index.md)

# SecurityProviderConfigurator

[jvm]\
object [SecurityProviderConfigurator](index.md)

Initializes JVM security providers at server startup.

## Functions

| Name | Summary |
|---|---|
| [initialize](initialize.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [initialize](initialize.md)()<br>Initialize security providers available statically; this method is typically run at server startup. |
| [ready](ready.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [ready](ready.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Indicate whether security providers have initialized. |
