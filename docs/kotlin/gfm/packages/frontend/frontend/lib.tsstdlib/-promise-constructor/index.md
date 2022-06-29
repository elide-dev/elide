//[frontend](../../../index.md)/[lib.tsstdlib](../index.md)/[PromiseConstructor](index.md)

# PromiseConstructor

[js]\
external interface [PromiseConstructor](index.md)

## Functions

| Name | Summary |
|---|---|
| [all](all.md) | [js]<br>abstract fun [all](all.md)(values: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;dynamic&gt;<br>abstract fun &lt;[T](all.md)&gt; [all](all.md)(values: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](all.md)&gt;&gt;<br>abstract fun &lt;[T](all.md)&gt; [all](all.md)(values: [Iterable](../-iterable/index.md)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](all.md)&gt;&gt; |
| [race](race.md) | [js]<br>abstract fun &lt;[T](race.md)&gt; [race](race.md)(values: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[T](race.md)&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>abstract fun &lt;[T](race.md)&gt; [race](race.md)(values: [Iterable](../-iterable/index.md)&lt;[T](race.md)&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>abstract fun &lt;[T](race.md)&gt; [race](race.md)(values: [Iterable](../-iterable/index.md)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[T](race.md)&gt; |
| [reject](reject.md) | [js]<br>abstract fun &lt;[T](reject.md)&gt; [reject](reject.md)(reason: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) = definedExternally): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[T](reject.md)&gt; |
| [resolve](resolve.md) | [js]<br>abstract fun [resolve](resolve.md)(): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)&gt;<br>abstract fun &lt;[T](resolve.md)&gt; [resolve](resolve.md)(value: [T](resolve.md)): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[T](resolve.md)&gt;<br>abstract fun &lt;[T](resolve.md)&gt; [resolve](resolve.md)(value: [PromiseLike](../-promise-like/index.md)&lt;[T](resolve.md)&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[T](resolve.md)&gt; |

## Properties

| Name | Summary |
|---|---|
| [prototype](prototype.md) | [js]<br>abstract var [prototype](prototype.md): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
