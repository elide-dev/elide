//[rpc-jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)

# ServiceHealthManager

[jvm]\
@Context

@Singleton

class [ServiceHealthManager](index.md)

Dedicated manager for service health signals; controls the central gRPC health checking service.

When a service is mounted via the [RpcRuntime](../../../../../packages/rpc-jvm/elide.server.rpc/-rpc-runtime/index.md), it is registered with the health service. Settings for Elide's RPC layer govern whether health methods are exposed to callers.

## Constructors

| | |
|---|---|
| [ServiceHealthManager](-service-health-manager.md) | [jvm]<br>fun [ServiceHealthManager](-service-health-manager.md)() |

## Functions

| Name | Summary |
|---|---|
| [currentStatus](current-status.md) | [jvm]<br>fun [currentStatus](current-status.md)(descriptor: ServiceDescriptor): HealthCheckResponse.ServingStatus<br>Query for the current service status for the service by the named service described by the provided [descriptor](current-status.md). If no status is available, return `UNKNOWN`.<br>[jvm]<br>fun [currentStatus](current-status.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HealthCheckResponse.ServingStatus<br>Query for the current service status for the service at [name](current-status.md). If no status is available, return `UNKNOWN`. |
| [notify](notify.md) | [jvm]<br>fun [notify](notify.md)(service: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), status: HealthCheckResponse.ServingStatus?)<br>Notify the central service health system that the provided [service](notify.md) should *currently* be considered under the provided [status](notify.md); all health-check calls after this moment should reflect the aforementioned state. Any current status for the service, as applicable, is cleared and replaced. |
| [notifyNotServing](notify-not-serving.md) | [jvm]<br>fun [notifyNotServing](notify-not-serving.md)(service: ServiceDescriptor)<br>Notify the central service health system that the provided [service](notify-not-serving.md) is currently in a `NOT_SERVING` state; a corresponding ServingStatus will be used under the hood. |
| [notifyPending](notify-pending.md) | [jvm]<br>fun [notifyPending](notify-pending.md)(service: ServiceDescriptor)<br>Notify the central service health system that the provided [service](notify-pending.md) is currently in a `PENDING` state; a corresponding ServingStatus will be used under the hood. |
| [notifyServing](notify-serving.md) | [jvm]<br>fun [notifyServing](notify-serving.md)(service: ServiceDescriptor)<br>Notify the central service health system that the provided [service](notify-serving.md) is currently in a `SERVING` state; a corresponding ServingStatus will be used under the hood. |
| [notifyUnknown](notify-unknown.md) | [jvm]<br>fun [notifyUnknown](notify-unknown.md)(service: ServiceDescriptor)<br>Notify the central service health system that the provided [service](notify-unknown.md) is currently in an `UNKNOWN` state; a corresponding ServingStatus will be used under the hood. |
| [terminalShutdown](terminal-shutdown.md) | [jvm]<br>fun [terminalShutdown](terminal-shutdown.md)()<br>Notify the central service health system that the API service is experiencing a total and terminal shutdown, which should result in negative-status calls for all services queried on the health service. **This state is not recoverable,** and should only be used for system shutdown events. |

## Properties

| Name | Summary |
|---|---|
| [service](service.md) | [jvm]<br>val [service](service.md): BindableService |
