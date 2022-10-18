//[test](../../../index.md)/[elide.server](../index.md)/[ElideServerTest](index.md)/[exchange](exchange.md)

# exchange

[jvm]\
fun [exchange](exchange.md)(request: MutableHttpRequest&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = 200, block: HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): HttpResponse&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;

Exchange a test [request](exchange.md) with the current server, expecting [status](exchange.md) as an HTTP return status; after executing the request to obtain the response, [block](exchange.md) is executed (if provided) to perform additional assertions on the response.

In this case, no response payload type is provided, and the request payload type is also generic. If you would like to decode the response and test against it, see other variants of this method.

#### Return

HTTP response from the server.

#### See also

jvm

| | |
|---|---|
| [ElideServerTest.exchange](exchange.md) | which allows a response body type. |

#### Parameters

jvm

| | |
|---|---|
| request | HTTP request to submit to the current Elide server. |
| status | Expected HTTP status for the response. |
| block | Block of assertions to additionally perform. |

[jvm]\
fun &lt;[P](exchange.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html), [R](exchange.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [exchange](exchange.md)(request: MutableHttpRequest&lt;[P](exchange.md)&gt;, status: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)? = 200, responseType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](exchange.md)&gt;, block: HttpResponse&lt;[R](exchange.md)&gt;.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): HttpResponse&lt;[R](exchange.md)&gt;

Exchange a test [request](exchange.md) with the current server, expecting [status](exchange.md) as an HTTP return status; after executing the request to obtain the response, [block](exchange.md) is executed (if provided) to perform additional assertions on the response.

#### Return

HTTP response from the server.

#### Parameters

jvm

| | |
|---|---|
| request | HTTP request to submit to the current Elide server. |
| status | Expected HTTP status for the response. |
| responseType | Type of the response payload. |
| block | Block of assertions to additionally perform. |
