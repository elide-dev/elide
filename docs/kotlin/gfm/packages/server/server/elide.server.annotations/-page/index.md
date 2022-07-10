//[server](../../../index.md)/[elide.server.annotations](../index.md)/[Page](index.md)

# Page

[jvm]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-c-l-a-s-s/index.html)])

@Bean

@Controller

@DefaultScope(value = Singleton::class)

annotation class [Page](index.md)(val route: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UriMapping.DEFAULT_URI, val produces: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = [MediaType.TEXT_HTML], val consumes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = [MediaType.TEXT_HTML])

Annotations which is applied to handlers which respond to HTTP requests with HTTP responses, typically encoded in HTML, for the purpose of presenting a user interface.

The Micronaut annotations `Controller` behaves in essentially the same manner as `Page`, but with different defaults and available settings.

## Parameters

jvm

| | |
|---|---|
| route | HTTP route that should be bound to this page. |

## Constructors

| | |
|---|---|
| [Page](-page.md) | [jvm]<br>fun [Page](-page.md)(route: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UriMapping.DEFAULT_URI, produces: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = [MediaType.TEXT_HTML], consumes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = [MediaType.TEXT_HTML]) |

## Properties

| Name | Summary |
|---|---|
| [consumes](consumes.md) | [jvm]<br>@get:AliasFor(annotation = Consumes::class, member = &quot;value&quot;)<br>val [consumes](consumes.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Content-Type consumed by this endpoint; defaults to JSON. |
| [produces](produces.md) | [jvm]<br>@get:AliasFor(annotation = Produces::class, member = &quot;value&quot;)<br>val [produces](produces.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Content-Type produced by this endpoint; defaults to HTML. |
| [route](route.md) | [jvm]<br>@get:AliasFor(annotation = UriMapping::class, member = &quot;value&quot;)<br>val [route](route.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>HTTP route that should be bound to this page. |
