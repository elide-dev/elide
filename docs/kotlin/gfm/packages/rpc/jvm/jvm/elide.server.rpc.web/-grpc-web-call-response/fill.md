//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebCallResponse](index.md)/[fill](fill.md)

# fill

[jvm]\
abstract fun [fill](fill.md)(response: MutableHttpResponse&lt;[RawRpcPayload](../index.md#-571776252%2FClasslikes%2F594929262)&gt;)

Fill out the provided HTTP [response](fill.md) with data attached to this call response state; the response is expected to comply with the structure stipulated by the [gRPC Web Protocol](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md) document.

## Parameters

jvm

| | |
|---|---|
| response | Mutable response to fill with information based on this response. |
