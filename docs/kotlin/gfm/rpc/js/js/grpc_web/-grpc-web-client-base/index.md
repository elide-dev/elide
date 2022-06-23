//[js](../../../index.md)/[grpc_web](../index.md)/[GrpcWebClientBase](index.md)

# GrpcWebClientBase

[js]\
open external class [GrpcWebClientBase](index.md)(options: [GrpcWebClientBaseOptions](../-grpc-web-client-base-options/index.md) = definedExternally) : [AbstractClientBase](../-abstract-client-base/index.md)

## Constructors

| | |
|---|---|
| [GrpcWebClientBase](-grpc-web-client-base.md) | [js]<br>fun [GrpcWebClientBase](-grpc-web-client-base.md)(options: [GrpcWebClientBaseOptions](../-grpc-web-client-base-options/index.md) = definedExternally) |

## Functions

| Name | Summary |
|---|---|
| [rpcCall](../-abstract-client-base/rpc-call.md) | [js]<br>open fun &lt;[REQ](../-abstract-client-base/rpc-call.md), [RESP](../-abstract-client-base/rpc-call.md)&gt; [rpcCall](../-abstract-client-base/rpc-call.md)(method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: [REQ](../-abstract-client-base/rpc-call.md), metadata: [Metadata](../-metadata/index.md), methodDescriptor: [MethodDescriptor](../-method-descriptor/index.md)&lt;[REQ](../-abstract-client-base/rpc-call.md), [RESP](../-abstract-client-base/rpc-call.md)&gt;, callback: ([RpcError](../index.md#-784981774%2FClasslikes%2F234436643), response: [RESP](../-abstract-client-base/rpc-call.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [ClientReadableStream](../-client-readable-stream/index.md)&lt;[RESP](../-abstract-client-base/rpc-call.md)&gt; |
| [serverStreaming](../-abstract-client-base/server-streaming.md) | [js]<br>open fun &lt;[REQ](../-abstract-client-base/server-streaming.md), [RESP](../-abstract-client-base/server-streaming.md)&gt; [serverStreaming](../-abstract-client-base/server-streaming.md)(method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: [REQ](../-abstract-client-base/server-streaming.md), metadata: [Metadata](../-metadata/index.md), methodDescriptor: [MethodDescriptor](../-method-descriptor/index.md)&lt;[REQ](../-abstract-client-base/server-streaming.md), [RESP](../-abstract-client-base/server-streaming.md)&gt;): [ClientReadableStream](../-client-readable-stream/index.md)&lt;[RESP](../-abstract-client-base/server-streaming.md)&gt; |
| [thenableCall](../-abstract-client-base/thenable-call.md) | [js]<br>open fun &lt;[REQ](../-abstract-client-base/thenable-call.md), [RESP](../-abstract-client-base/thenable-call.md)&gt; [thenableCall](../-abstract-client-base/thenable-call.md)(method: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: [REQ](../-abstract-client-base/thenable-call.md), metadata: [Metadata](../-metadata/index.md), methodDescriptor: [MethodDescriptor](../-method-descriptor/index.md)&lt;[REQ](../-abstract-client-base/thenable-call.md), [RESP](../-abstract-client-base/thenable-call.md)&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[RESP](../-abstract-client-base/thenable-call.md)&gt; |
