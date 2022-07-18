//[server](../../../index.md)/[elide.server.type](../index.md)/[RequestState](index.md)

# RequestState

[jvm]\
data class [RequestState](index.md)(val request: HttpRequest&lt;*&gt;, val principal: [Principal](https://docs.oracle.com/javase/8/docs/api/java/security/Principal.html)?)

Request state container which is passed to methods which need access to request state.

## Parameters

jvm

| | |
|---|---|
| request | HTTP request bound to this request state. |
| principal | Security principal detected for this request, or `null`. |

## Constructors

| | |
|---|---|
| [RequestState](-request-state.md) | [jvm]<br>fun [RequestState](-request-state.md)(request: HttpRequest&lt;*&gt;, principal: [Principal](https://docs.oracle.com/javase/8/docs/api/java/security/Principal.html)?) |

## Properties

| Name | Summary |
|---|---|
| [principal](principal.md) | [jvm]<br>val [principal](principal.md): [Principal](https://docs.oracle.com/javase/8/docs/api/java/security/Principal.html)? |
| [request](request.md) | [jvm]<br>val [request](request.md): HttpRequest&lt;*&gt; |
