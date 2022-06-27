//[rpc-jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[notifyPending](notify-pending.md)

# notifyPending

[jvm]\
fun [notifyPending](notify-pending.md)(service: ServiceDescriptor)

Notify the central service health system that the provided [service](notify-pending.md) is currently in a `PENDING` state; a corresponding ServingStatus will be used under the hood.

## See also

jvm

| | |
|---|---|
| [elide.server.rpc.ServiceHealthManager](notify.md) | for full control over status reporting. |

## Parameters

jvm

| | |
|---|---|
| service | Descriptor for the service we are reporting status for. |
