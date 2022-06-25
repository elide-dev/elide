//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebController](index.md)/[handleRequest](handle-request.md)

# handleRequest

[jvm]\

@Post(value = &quot;/{servicePath}/{methodName}&quot;)

suspend fun [handleRequest](handle-request.md)(servicePath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), methodName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: HttpRequest&lt;[RawRpcPayload](../index.md#-571776252%2FClasslikes%2F594929262)&gt;, principal: [Principal](https://docs.oracle.com/javase/8/docs/api/java/security/Principal.html)?): HttpResponse&lt;[RawRpcPayload](../index.md#-571776252%2FClasslikes%2F594929262)&gt;

Main RPC request handler; all gRPC-Web requests are expected to operate over `POST`.

This is the effective entrypoint for gRPC-Web traffic handled by Elide on behalf of a framework user's application. Messages flow in from the JavaScript or Kotlin RPC client layer, encoded as protocol buffer messages, either in binary form or wrapped in Base64.

###  Method Resolution

The handler calls into [RpcRuntime](../../../../../../packages/rpc/jvm/elide.server.rpc/-rpc-runtime/index.md) to resolve the gRPC service named at the provided [servicePath](handle-request.md). The method described by [methodName](handle-request.md) is then resolved from the generated gRPC service descriptor. If either of these steps fail, the handler will return an HttpResponse describing Status.NOT_FOUND, with an error message packed in the `grpc-message` header describing what went wrong.

###  Error handling

Unhandled and/or unrecognized exceptions will be translated into their gRPC equivalent, carrying a status value of Status.INTERNAL. Any exception which is not a StatusRuntimeException will be considered &quot;unrecognized.&quot; Inheritors of StatusRuntimeException preserve their status and trailers.

#### Return

HTTP response to be sent back to the invoking client.

## See also

jvm

| | |
|---|---|
| [elide.server.rpc.RpcRuntime](../../../../../../packages/rpc/jvm/elide.server.rpc/-rpc-runtime/index.md) | for service and method resolution details. |

## Parameters

jvm

| | |
|---|---|
| servicePath | Fully-qualified path for the gRPC service under dispatch. |
| methodName | Name of the method under dispatch. |
| request | HTTP request which is incoming to the RPC endpoint, and which should be processed as a potential gRPC-Web invocation. |
| principal | Logged-in security principal, if any. |

## Throws

| | |
|---|---|
| io.grpc.StatusRuntimeException | if the service or method cannot be located, or if the request cannot be decoded, or if any runtime error occurs. Any exception that *does not* inherit from this type should be considered unhandled. |
