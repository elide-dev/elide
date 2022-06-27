//[rpc-jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[currentStatus](current-status.md)

# currentStatus

[jvm]\
fun [currentStatus](current-status.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HealthCheckResponse.ServingStatus

Query for the current service status for the service at [name](current-status.md). If no status is available, return `UNKNOWN`.

#### Return

Current serving status, or ServingStatus.UNKNOWN if unknown.

## Parameters

jvm

| | |
|---|---|
| name | Name of the service we wish to query status for. |

[jvm]\
fun [currentStatus](current-status.md)(descriptor: ServiceDescriptor): HealthCheckResponse.ServingStatus

Query for the current service status for the service by the named service described by the provided [descriptor](current-status.md). If no status is available, return `UNKNOWN`.

#### Return

Current serving status, or ServingStatus.UNKNOWN if unknown.

## Parameters

jvm

| | |
|---|---|
| descriptor | Service descriptor to query status for. |
