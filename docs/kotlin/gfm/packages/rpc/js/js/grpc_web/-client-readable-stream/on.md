//[js](../../../index.md)/[grpc_web](../index.md)/[ClientReadableStream](index.md)/[on](on.md)

# on

[js]\
open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: ([RpcError](../index.md#-784981774%2FClasslikes%2F754089342)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Status](../-status/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (status: [Metadata](../-metadata/index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: (response: [RESP](index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;

open fun [on](on.md)(eventType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), callback: () -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](index.md)&lt;[RESP](index.md)&gt;
