//[rpc-js](../../index.md)/[lib.grpcweb](index.md)

# Package lib.grpcweb

## Types

| Name | Summary |
|---|---|
| [AbstractClientBase](-abstract-client-base/index.md) | [js]<br>open external class [AbstractClientBase](-abstract-client-base/index.md) |
| [CallOptions](-call-options/index.md) | [js]<br>open external class [CallOptions](-call-options/index.md)(options: [Json](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.js/-json/index.html)) |
| [ClientReadableStream](-client-readable-stream/index.md) | [js]<br>open external class [ClientReadableStream](-client-readable-stream/index.md)&lt;[RESP](-client-readable-stream/index.md)&gt; |
| [GrpcWebClientBase](-grpc-web-client-base/index.md) | [js]<br>open external class [GrpcWebClientBase](-grpc-web-client-base/index.md)(options: [GrpcWebClientBaseOptions](-grpc-web-client-base-options/index.md) = definedExternally) : [AbstractClientBase](-abstract-client-base/index.md) |
| [GrpcWebClientBaseOptions](-grpc-web-client-base-options/index.md) | [js]<br>external interface [GrpcWebClientBaseOptions](-grpc-web-client-base-options/index.md) |
| [Metadata](-metadata/index.md) | [js]<br>external interface [Metadata](-metadata/index.md) |
| [MethodDescriptor](-method-descriptor/index.md) | [js]<br>open external class [MethodDescriptor](-method-descriptor/index.md)&lt;[REQ](-method-descriptor/index.md), [RESP](-method-descriptor/index.md)&gt;(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), methodType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), requestType: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), responseType: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), requestSerializeFn: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), responseDeserializeFn: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)) |
| [Request](-request/index.md) | [js]<br>open external class [Request](-request/index.md)&lt;[REQ](-request/index.md), [RESP](-request/index.md)&gt; |
| [RpcError](index.md#2067006156%2FClasslikes%2F854961009) | [js]<br>typealias [RpcError](index.md#2067006156%2FClasslikes%2F854961009) = [Error](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-error/index.html) |
| [Status](-status/index.md) | [js]<br>external interface [Status](-status/index.md) |
| [StatusCode](-status-code/index.md) | [js]<br>external enum [StatusCode](-status-code/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[StatusCode](-status-code/index.md)&gt; |
| [StreamInterceptor](-stream-interceptor/index.md) | [js]<br>external interface [StreamInterceptor](-stream-interceptor/index.md)&lt;[REQ](-stream-interceptor/index.md), [RESP](-stream-interceptor/index.md)&gt; |
| [UnaryInterceptor](-unary-interceptor/index.md) | [js]<br>external interface [UnaryInterceptor](-unary-interceptor/index.md)&lt;[REQ](-unary-interceptor/index.md), [RESP](-unary-interceptor/index.md)&gt; |
| [UnaryResponse](-unary-response/index.md) | [js]<br>open external class [UnaryResponse](-unary-response/index.md)&lt;[REQ](-unary-response/index.md), [RESP](-unary-response/index.md)&gt; |

## Properties

| Name | Summary |
|---|---|
| [SERVER_STREAMING](-s-e-r-v-e-r_-s-t-r-e-a-m-i-n-g.md) | [js]<br>external var [SERVER_STREAMING](-s-e-r-v-e-r_-s-t-r-e-a-m-i-n-g.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [UNARY](-u-n-a-r-y.md) | [js]<br>external var [UNARY](-u-n-a-r-y.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
