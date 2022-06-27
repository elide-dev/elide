//[jvm](../../../index.md)/[elide.server.rpc](../index.md)/[ServiceHealthManager](index.md)/[notifyNotServing](notify-not-serving.md)

# notifyNotServing

[jvm]\
fun [notifyNotServing](notify-not-serving.md)(service: ServiceDescriptor)

Notify the central service health system that the provided [service](notify-not-serving.md) is currently in a `NOT_SERVING` state; a corresponding ServingStatus will be used under the hood.

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
