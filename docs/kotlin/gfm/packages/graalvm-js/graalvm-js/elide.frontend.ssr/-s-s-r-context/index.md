//[graalvm-js](../../../index.md)/[elide.frontend.ssr](../index.md)/[SSRContext](index.md)

# SSRContext

[js]\
class [SSRContext](index.md)&lt;[State](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

Context access utility for SSR-shared state.

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [js]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [execute](execute.md) | [js]<br>fun &lt;[R](execute.md)&gt; [execute](execute.md)(fn: [SSRContext](index.md)&lt;[State](index.md)&gt;.() -&gt; [R](execute.md)): [R](execute.md)<br>Execute the provided [fn](execute.md) within the context of this decoded SSR context. |

## Properties

| Name | Summary |
|---|---|
| [state](state.md) | [js]<br>val [state](state.md): [State](index.md)? |
