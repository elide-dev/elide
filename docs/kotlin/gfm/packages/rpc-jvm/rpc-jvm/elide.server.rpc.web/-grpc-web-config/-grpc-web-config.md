//[rpc-jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebConfig](index.md)/[GrpcWebConfig](-grpc-web-config.md)

# GrpcWebConfig

[jvm]\
fun [GrpcWebConfig](-grpc-web-config.md)(enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, endpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = defaultEndpoint, timeout: [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) = Duration.ofSeconds(30))

## Parameters

jvm

| | |
|---|---|
| enabled | Whether gRPC Web support is enabled. |
| endpoint | Base URI where RPC requests should be handled by the built-in controller. |
