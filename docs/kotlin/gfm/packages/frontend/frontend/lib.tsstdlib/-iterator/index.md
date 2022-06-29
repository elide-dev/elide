//[frontend](../../../index.md)/[lib.tsstdlib](../index.md)/[Iterator](index.md)

# Iterator

[js]\
external interface [Iterator](index.md)&lt;[T](index.md), [TReturn](index.md), [TNext](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [next](next.md) | [js]<br>abstract fun [next](next.md)(vararg args: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)): dynamic |

## Properties

| Name | Summary |
|---|---|
| [return](return.md) | [js]<br>abstract val [return](return.md): (value: [TReturn](index.md)) -&gt; dynamic? |
| [throw](throw.md) | [js]<br>abstract val [throw](throw.md): (e: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) -&gt; dynamic? |
