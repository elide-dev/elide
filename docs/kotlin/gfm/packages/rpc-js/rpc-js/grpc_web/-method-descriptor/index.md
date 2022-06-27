//[rpc-js](../../../index.md)/[grpc_web](../index.md)/[MethodDescriptor](index.md)

# MethodDescriptor

[js]\
open external class [MethodDescriptor](index.md)&lt;[REQ](index.md), [RESP](index.md)&gt;(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), methodType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestType: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), responseType: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), requestSerializeFn: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), responseDeserializeFn: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html))

## Constructors

| | |
|---|---|
| [MethodDescriptor](-method-descriptor.md) | [js]<br>fun [MethodDescriptor](-method-descriptor.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), methodType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestType: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), responseType: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), requestSerializeFn: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), responseDeserializeFn: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [createRequest](create-request.md) | [js]<br>open fun [createRequest](create-request.md)(requestMessage: [REQ](index.md), metadata: [Metadata](../-metadata/index.md) = definedExternally, callOptions: [CallOptions](../-call-options/index.md) = definedExternally): [Request](../-request/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt; |
| [createUnaryResponse](create-unary-response.md) | [js]<br>open fun [createUnaryResponse](create-unary-response.md)(responseMessage: [RESP](index.md), metadata: [Metadata](../-metadata/index.md) = definedExternally, status: [Status](../-status/index.md) = definedExternally): [UnaryResponse](../-unary-response/index.md)&lt;[REQ](index.md), [RESP](index.md)&gt; |
| [getMethodType](get-method-type.md) | [js]<br>open fun [getMethodType](get-method-type.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getName](get-name.md) | [js]<br>open fun [getName](get-name.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [getRequestMessageCtor](get-request-message-ctor.md) | [js]<br>open fun [getRequestMessageCtor](get-request-message-ctor.md)(): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getRequestSerializeFn](get-request-serialize-fn.md) | [js]<br>open fun [getRequestSerializeFn](get-request-serialize-fn.md)(): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getResponseDeserializeFn](get-response-deserialize-fn.md) | [js]<br>open fun [getResponseDeserializeFn](get-response-deserialize-fn.md)(): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
| [getResponseMessageCtor](get-response-message-ctor.md) | [js]<br>open fun [getResponseMessageCtor](get-response-message-ctor.md)(): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) |
