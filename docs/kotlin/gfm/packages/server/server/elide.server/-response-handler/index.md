//[server](../../../index.md)/[elide.server](../index.md)/[ResponseHandler](index.md)

# ResponseHandler

[jvm]\
interface [ResponseHandler](index.md)&lt;[ResponseBody](index.md)&gt;

Describes a handler object which can respond to a request with a given [ResponseBody](index.md) type; these throw-away handlers are typically spawned by utility functions to create a context where rendering can take place.

## Functions

| Name | Summary |
|---|---|
| [respond](respond.md) | [jvm]<br>abstract suspend fun [respond](respond.md)(response: HttpResponse&lt;[ResponseBody](index.md)&gt;): HttpResponse&lt;[ResponseBody](index.md)&gt;<br>Respond to the request with the provided [response](respond.md). |

## Inheritors

| Name |
|---|
| [BaseResponseHandler](../-base-response-handler/index.md) |
