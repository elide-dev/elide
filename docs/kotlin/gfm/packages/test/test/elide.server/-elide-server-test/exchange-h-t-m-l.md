//[test](../../../index.md)/[elide.server](../index.md)/[ElideServerTest](index.md)/[exchangeHTML](exchange-h-t-m-l.md)

# exchangeHTML

[jvm]\
fun [exchangeHTML](exchange-h-t-m-l.md)(request: MutableHttpRequest&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, accept: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = &quot;text/html,*/*&quot;, status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = 200, block: HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;.(Document) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

Syntactic sugar to exchange a [request](exchange-h-t-m-l.md) with the active Elide server which is expected to produce HTML; assertions are run against the response (expected status, content-type/encoding/length, etc), and then the page is parsed with Jsoup and handed to [block](exchange-h-t-m-l.md) for additional testing.

If any step of the parsing or fetching process fails, the test invoking this method also fails (unless such errors are caught by the invoker).

#### Return

HTTP response from the server.

## See also

jvm

| | |
|---|---|
| [elide.server.ElideServerTest](exchange.md) | which allows an arbitrary response body type. |

## Parameters

jvm

| | |
|---|---|
| request | HTTP request to submit to the current Elide server. |
| accept | Accept value to append to the response; if `null` is passed, nothing is appended. |
| status | Expected HTTP status for the response. |
| block | Block of assertions to additionally perform, with access to the parsed page. |
