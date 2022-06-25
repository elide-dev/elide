//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebController](index.md)

# GrpcWebController

[jvm]\
@Requires(property = &quot;elide.grpc.web.enabled&quot;, value = &quot;true&quot;)

@Controller(value = &quot;${elide.grpc.web.endpoint:/_/rpc}&quot;)

class [GrpcWebController](index.md) : [StatusEnabledController](../../../../../../packages/server/server/elide.server.controller/-status-enabled-controller/index.md)

Entrypoint controller for gRPC Web traffic handled on behalf of the user's application by Elide's RPC framework.

This controller is responsible for resolving the appropriate service to dispatch, enforcing expectations against the incoming request, and performing serialization back and forth between the browser and backing gRPC service. Each of these functions is implemented in other classes, but all are driven via this controller.

Services are resolved via [RpcRuntime](../../../../../../packages/rpc/jvm/elide.server.rpc/-rpc-runtime/index.md), which is notified of service registration at the time the gRPC server starts up (managed by Micronaut).

## Constructors

| | |
|---|---|
| [GrpcWebController](-grpc-web-controller.md) | [jvm]<br>fun [GrpcWebController](-grpc-web-controller.md)() |

## Functions

| Name | Summary |
|---|---|
| [handleRequest](handle-request.md) | [jvm]<br>@Post(value = &quot;/{servicePath}/{methodName}&quot;)<br>suspend fun [handleRequest](handle-request.md)(servicePath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), methodName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), request: HttpRequest&lt;[RawRpcPayload](../index.md#-571776252%2FClasslikes%2F594929262)&gt;, principal: [Principal](https://docs.oracle.com/javase/8/docs/api/java/security/Principal.html)?): HttpResponse&lt;[RawRpcPayload](../index.md#-571776252%2FClasslikes%2F594929262)&gt;<br>Main RPC request handler; all gRPC-Web requests are expected to operate over `POST`. |

## Properties

| Name | Summary |
|---|---|
| [settings](settings.md) | [jvm]<br>@Inject<br>lateinit var [settings](settings.md): [GrpcWebConfig](../-grpc-web-config/index.md)<br>Configuration settings interpreted from the current application. |
