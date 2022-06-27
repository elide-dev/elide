//[rpc-jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[notifyUnknown](notify-unknown.md)

# notifyUnknown

[jvm]\
fun [notifyUnknown](notify-unknown.md)(service: ServiceDescriptor)

Notify the central service health system that the provided [service](notify-unknown.md) is currently in an `UNKNOWN` state; a corresponding ServingStatus will be used under the hood.

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
