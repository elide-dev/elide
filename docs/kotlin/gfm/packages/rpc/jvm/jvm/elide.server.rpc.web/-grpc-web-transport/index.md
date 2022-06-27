//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebTransport](index.md)

# GrpcWebTransport

[jvm]\
interface [GrpcWebTransport](index.md)

Describes the provided API surface for a manager which holds a connection to a backing gRPC server, which is used to fulfill requests on behalf of Elide's gRPC Web integration.

This manager is typically used by the [GrpcWebService](../../../../../../packages/rpc/jvm/elide.server.rpc.web/-grpc-web-service/index.md) to maintain an open connection (or connection pool) to the fulfilling server. During testing, this interface may be overridden to mock the backing server without needing network access.

## Functions

| Name | Summary |
|---|---|
| [channel](channel.md) | [jvm]<br>abstract fun [channel](channel.md)(): ManagedChannel<br>Create or acquire a gRPC channel which the consumer can use to interact with the gRPC server backing this service. |
