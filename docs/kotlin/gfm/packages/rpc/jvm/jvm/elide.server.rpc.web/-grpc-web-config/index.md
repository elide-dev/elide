//[jvm](../../../index.md)/[elide.server.rpc.web](../index.md)/[GrpcWebConfig](index.md)

# GrpcWebConfig

[jvm]\
@ConfigurationProperties(value = &quot;elide.grpc.web&quot;)

data class [GrpcWebConfig](index.md)(var enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, var endpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = defaultEndpoint, var timeout: [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) = Duration.ofSeconds(30))

Describes active configuration for Elide's RPC layer as related to integration with gRPC Web.

## Parameters

jvm

| | |
|---|---|
| enabled | Whether gRPC Web support is enabled. |
| endpoint | Base URI where RPC requests should be handled by the built-in controller. |

## Constructors

| | |
|---|---|
| [GrpcWebConfig](-grpc-web-config.md) | [jvm]<br>fun [GrpcWebConfig](-grpc-web-config.md)(enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, endpoint: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = defaultEndpoint, timeout: [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) = Duration.ofSeconds(30)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [jvm]<br>var [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [endpoint](endpoint.md) | [jvm]<br>var [endpoint](endpoint.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [timeout](timeout.md) | [jvm]<br>var [timeout](timeout.md): [Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) |
