//[rpc-js](../../../index.md)/[lib.grpcweb](../index.md)/[ClientReadableStream](index.md)/[on](on.md)

# on

[js]\
open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: ([RpcError](../index.md#2067006156%2FClasslikes%2F854961009)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Status](../-status/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Metadata](../-metadata/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (response: [RESP](index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;
