//[server](../../../index.md)/[elide.server.controller.builtin](../index.md)/[NotFoundController](index.md)/[handle](handle.md)

# handle

[jvm]\

@Get(value = &quot;/error/notfound&quot;, produces = [&quot;text/html&quot;, &quot;application/json&quot;])

@Error(status = HttpStatus.NOT_FOUND, global = true)

open suspend override fun [handle](handle.md)(request: HttpRequest&lt;out [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;): [RawResponse](../../elide.server/index.md#852884585%2FClasslikes%2F-1343588467)
