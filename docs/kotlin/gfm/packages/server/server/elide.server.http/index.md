//[server](../../index.md)/[elide.server.http](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [HeaderFinalizingFilter](-header-finalizing-filter/index.md) | [jvm]<br>@Filter(value = [&quot;/**&quot;])<br>class [HeaderFinalizingFilter](-header-finalizing-filter/index.md) : HttpServerFilter<br>Provides an HttpServerFilter which cleans response headers by de-duplicating certain values, ensuring consistent casing, and applying settings specified by the developer within static configuration. |
| [RequestContext](-request-context/index.md) | [jvm]<br>object [RequestContext](-request-context/index.md)<br>Effective namespace for request context values and objects. |
| [RequestContextFilter](-request-context-filter/index.md) | [jvm]<br>@Filter(value = [&quot;/**&quot;])<br>class [RequestContextFilter](-request-context-filter/index.md) : HttpServerFilter<br>Provides an HttpServerFilter which affixes context values at known keys in HttpRequests processed by Elide apps; known keys are defined via [RequestContext.Key](-request-context/-key/index.md). |
