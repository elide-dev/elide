//[rpc-jvm](../../../../index.md)/[elide.server.rpc.web](../../index.md)/[GrpcWeb](../index.md)/[Metadata](index.md)

# Metadata

[jvm]\
object [Metadata](index.md)

Special metadata keys.

## Properties

| Name | Summary |
|---|---|
| [apiKey](api-key.md) | [jvm]<br>val [apiKey](api-key.md): Metadata.Key&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>User-provided `x-api-key` header. |
| [authorization](authorization.md) | [jvm]<br>val [authorization](authorization.md): Metadata.Key&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>User-provided `authorization` header. |
| [internalCall](internal-call.md) | [jvm]<br>val [internalCall](internal-call.md): Metadata.Key&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Key which is used to signify an internal gRPC Web call to the backing server. |
| [trace](trace.md) | [jvm]<br>val [trace](trace.md): Metadata.Key&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Trace header. |
