//[server](../../../index.md)/[elide.server.http](../index.md)/[HeaderFinalizingFilter](index.md)

# HeaderFinalizingFilter

[jvm]\
@Filter(value = [&quot;/**&quot;])

class [HeaderFinalizingFilter](index.md) : HttpServerFilter

Provides an HttpServerFilter which cleans response headers by de-duplicating certain values, ensuring consistent casing, and applying settings specified by the developer within static configuration.

The header finalizing filter does not touch headers except ones which are registered on a local allow-list.

## Constructors

| | |
|---|---|
| [HeaderFinalizingFilter](-header-finalizing-filter.md) | [jvm]<br>fun [HeaderFinalizingFilter](-header-finalizing-filter.md)() |

## Functions

| Name | Summary |
|---|---|
| [doFilter](do-filter.md) | [jvm]<br>open override fun [doFilter](do-filter.md)(request: HttpRequest&lt;*&gt;, chain: ServerFilterChain): Publisher&lt;MutableHttpResponse&lt;*&gt;&gt;<br>open override fun [doFilter](../-request-context-filter/index.md#-1838193004%2FFunctions%2F-1343588467)(request: HttpRequest&lt;*&gt;, chain: FilterChain): Publisher&lt;out HttpResponse&lt;*&gt;&gt; |
| [getOrder](../-request-context-filter/index.md#785826419%2FFunctions%2F-1343588467) | [jvm]<br>open fun [getOrder](../-request-context-filter/index.md#785826419%2FFunctions%2F-1343588467)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
