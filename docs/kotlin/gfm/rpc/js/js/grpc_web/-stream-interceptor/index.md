//[js](../../../index.md)/[grpc_web](../index.md)/[StreamInterceptor](index.md)

# StreamInterceptor

[js]\
external interface [StreamInterceptor](index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [intercept](intercept.md) | [js]<br>abstract fun [intercept](intercept.md)(request: [Request](../-request/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;, invoker: (request: [Request](../-request/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;) -&gt; [ClientReadableStream](../-client-readable-stream/index.md)&lt;[RESP](index.md)&gt;): [ClientReadableStream](../-client-readable-stream/index.md)&lt;[RESP](index.md)&gt; |
