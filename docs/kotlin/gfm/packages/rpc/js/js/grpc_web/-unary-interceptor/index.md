//[js](../../../index.md)/[grpc_web](../index.md)/[UnaryInterceptor](index.md)

# UnaryInterceptor

[js]\
external interface [UnaryInterceptor](index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [intercept](intercept.md) | [js]<br>abstract fun [intercept](intercept.md)(request: [Request](../-request/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;, invoker: (request: [Request](../-request/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;) -&gt; [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[UnaryResponse](../-unary-response/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;&gt;): [Promise](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-promise/index.html)&lt;[UnaryResponse](../-unary-response/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;&gt; |
