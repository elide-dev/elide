//[test](../../../index.md)/[elide.server](../index.md)/[ElideServerTest](index.md)

# ElideServerTest

[jvm]\
abstract class [ElideServerTest](index.md)

Base class for Micronaut tests which want to use the enclosed convenience functions.

## Constructors

| | |
|---|---|
| [ElideServerTest](-elide-server-test.md) | [jvm]<br>fun [ElideServerTest](-elide-server-test.md)() |

## Functions

| Name | Summary |
|---|---|
| [exchange](exchange.md) | [jvm]<br>fun [exchange](exchange.md)(request: MutableHttpRequest&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = 200, block: HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>fun &lt;[P](exchange.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](exchange.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [exchange](exchange.md)(request: MutableHttpRequest&lt;[P](exchange.md)&gt;, status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = 200, responseType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](exchange.md)&gt;, block: HttpResponse&lt;[R](exchange.md)&gt;.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): HttpResponse&lt;[R](exchange.md)&gt;<br>Exchange a test [request](exchange.md) with the current server, expecting [status](exchange.md) as an HTTP return status; after executing the request to obtain the response, [block](exchange.md) is executed (if provided) to perform additional assertions on the response. |
| [exchangeHTML](exchange-h-t-m-l.md) | [jvm]<br>fun [exchangeHTML](exchange-h-t-m-l.md)(request: MutableHttpRequest&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, accept: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = &quot;text/html,*/*&quot;, status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = 200, block: HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;.(Document) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;<br>Syntactic sugar to exchange a [request](exchange-h-t-m-l.md) with the active Elide server which is expected to produce HTML; assertions are run against the response (expected status, content-type/encoding/length, etc), and then the page is parsed with Jsoup and handed to [block](exchange-h-t-m-l.md) for additional testing. |

## Properties

| Name | Summary |
|---|---|
| [app](app.md) | [jvm]<br>@Inject<br>lateinit var [app](app.md): EmbeddedServer |
| [beanContext](bean-context.md) | [jvm]<br>@Inject<br>lateinit var [beanContext](bean-context.md): BeanContext |
| [client](client.md) | [jvm]<br>@Inject<br>lateinit var [client](client.md): HttpClient |
| [context](context.md) | [jvm]<br>@Inject<br>lateinit var [context](context.md): ApplicationContext |
