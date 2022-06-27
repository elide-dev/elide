//[rpc-js](../../../index.md)/[grpc_web](../index.md)/[AbstractClientBase](index.md)

# AbstractClientBase

[js]\
open external class [AbstractClientBase](index.md)

## Constructors

| | |
|---|---|
| [AbstractClientBase](-abstract-client-base.md) | [js]<br>fun [AbstractClientBase](-abstract-client-base.md)() |

## Functions

| Name | Summary |
|---|---|
| [rpcCall](rpc-call.md) | [js]<br>open fun &lt;[REQ](rpc-call.md), [RESP](rpc-call.md)&gt; [rpcCall](rpc-call.md)(method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: [REQ](rpc-call.md), metadata: [Metadata](../-metadata/index.md), methodDescriptor: [MethodDescriptor](../-method-descriptor/index.md)&lt;[REQ](rpc-call.md), [RESP](rpc-call.md)&gt;, callback: ([RpcError](../index.md#-784981774%2FClasslikes%2F854961009), response: [RESP](rpc-call.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](../-client-readable-stream/index.md)&lt;[RESP](rpc-call.md)&gt; |
| [serverStreaming](server-streaming.md) | [js]<br>open fun &lt;[REQ](server-streaming.md), [RESP](server-streaming.md)&gt; [serverStreaming](server-streaming.md)(method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: [REQ](server-streaming.md), metadata: [Metadata](../-metadata/index.md), methodDescriptor: [MethodDescriptor](../-method-descriptor/index.md)&lt;[REQ](server-streaming.md), [RESP](server-streaming.md)&gt;): [ClientReadableStream](../-client-readable-stream/index.md)&lt;[RESP](server-streaming.md)&gt; |
| [thenableCall](thenable-call.md) | [js]<br>open fun &lt;[REQ](thenable-call.md), [RESP](thenable-call.md)&gt; [thenableCall](thenable-call.md)(method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: [REQ](thenable-call.md), metadata: [Metadata](../-metadata/index.md), methodDescriptor: [MethodDescriptor](../-method-descriptor/index.md)&lt;[REQ](thenable-call.md), [RESP](thenable-call.md)&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[RESP](thenable-call.md)&gt; |

## Inheritors

| Name |
|---|
| [GrpcWebClientBase](../-grpc-web-client-base/index.md) |
