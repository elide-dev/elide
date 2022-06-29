//[rpc-js](../../../index.md)/[lib.grpcweb](../index.md)/[ClientReadableStream](index.md)

# ClientReadableStream

[js]\
open external class [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

## Constructors

| | |
|---|---|
| [ClientReadableStream](-client-readable-stream.md) | [js]<br>fun [ClientReadableStream](-client-readable-stream.md)() |

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [js]<br>open fun [cancel](cancel.md)() |
| [on](on.md) | [js]<br>open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;<br>open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (response: [RESP](index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;<br>open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: ([RpcError](../index.md#2067006156%2FClasslikes%2F854961009)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;<br>open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Metadata](../-metadata/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;<br>open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Status](../-status/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt; |
| [removeListener](remove-listener.md) | [js]<br>open fun [removeListener](remove-listener.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))<br>open fun [removeListener](remove-listener.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (response: [RESP](index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))<br>open fun [removeListener](remove-listener.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: ([RpcError](../index.md#2067006156%2FClasslikes%2F854961009)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))<br>open fun [removeListener](remove-listener.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Metadata](../-metadata/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))<br>open fun [removeListener](remove-listener.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Status](../-status/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
