//[server](../../../index.md)/[elide.server](../index.md)/[ResponseConfigurator](index.md)

# ResponseConfigurator

[jvm]\
fun interface [ResponseConfigurator](index.md)&lt;[Context](index.md) : [ResponseHandler](../-response-handler/index.md)&lt;[ResponseBody](index.md)&gt;, [RequestBody](index.md), [ResponseBody](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [respond](respond.md) | [jvm]<br>abstract fun [respond](respond.md)(handler: [Context](index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
