//[jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[notify](notify.md)

# notify

[jvm]\
fun [notify](notify.md)(service: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), status: HealthCheckResponse.ServingStatus?)

Notify the central service health system that the provided [service](notify.md) should *currently* be considered under the provided [status](notify.md); all health-check calls after this moment should reflect the aforementioned state. Any current status for the service, as applicable, is cleared and replaced.

Passing `null` for the [status](notify.md) value forcibly clears any active status for the specified [service](notify.md) (not recommended except in extreme circumstances).

## See also

jvm

| | |
|---|---|
| [elide.server.rpc.ServiceHealthManager](notify-unknown.md) | shorthand for unknown status. |

## Parameters

jvm

| | |
|---|---|
| service | Name for the service we are reporting status for. |
| status | Status we are reporting for the specified service. |
