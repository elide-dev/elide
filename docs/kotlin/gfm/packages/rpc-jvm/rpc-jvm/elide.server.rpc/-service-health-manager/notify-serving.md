//[rpc-jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[notifyServing](notify-serving.md)

# notifyServing

[jvm]\
fun [notifyServing](notify-serving.md)(service: ServiceDescriptor)

Notify the central service health system that the provided [service](notify-serving.md) is currently in a `SERVING` state; a corresponding ServingStatus will be used under the hood.

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
