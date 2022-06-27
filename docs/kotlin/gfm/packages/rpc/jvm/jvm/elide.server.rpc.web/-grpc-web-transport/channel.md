//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebTransport](index.md)/[channel](channel.md)

# channel

[jvm]\
abstract fun [channel](channel.md)(): ManagedChannel

Create or acquire a gRPC channel which the consumer can use to interact with the gRPC server backing this service.

No guarantee is provided that the return channel is different across method invocations, to allow for connection pooling and other internal operations.

#### Return

Managed channel that should be used to communicate with the server.
