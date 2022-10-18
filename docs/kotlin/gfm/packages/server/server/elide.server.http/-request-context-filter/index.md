//[server](../../../index.md)/[elide.server.http](../index.md)/[RequestContextFilter](index.md)

# RequestContextFilter

[jvm]\
@Filter(value = [&quot;/**&quot;])

class [RequestContextFilter](index.md) : HttpServerFilter

Provides an HttpServerFilter which affixes context values at known keys in HttpRequests processed by Elide apps; known keys are defined via [RequestContext.Key](../-request-context/-key/index.md).

#### See also

jvm

| | |
|---|---|
| [RequestContext.Key](../-request-context/-key/index.md) | for an exhaustive review of available request context. |

## Constructors

| | |
|---|---|
| [RequestContextFilter](-request-context-filter.md) | [jvm]<br>fun [RequestContextFilter](-request-context-filter.md)() |

## Functions

| Name | Summary |
|---|---|
| [doFilter](do-filter.md) | [jvm]<br>open override fun [doFilter](do-filter.md)(request: HttpRequest&lt;*&gt;, chain: ServerFilterChain): Publisher&lt;MutableHttpResponse&lt;*&gt;&gt;<br>open override fun [doFilter](index.md#-1838193004%2FFunctions%2F-1343588467)(request: HttpRequest&lt;*&gt;, chain: FilterChain): Publisher&lt;out HttpResponse&lt;*&gt;&gt; |
| [getOrder](index.md#785826419%2FFunctions%2F-1343588467) | [jvm]<br>open fun [getOrder](index.md#785826419%2FFunctions%2F-1343588467)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
