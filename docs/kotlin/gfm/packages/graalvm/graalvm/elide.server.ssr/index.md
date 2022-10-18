//[graalvm](../../index.md)/[elide.server.ssr](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [ServerRenderer](-server-renderer/index.md) | [jvm]<br>interface [ServerRenderer](-server-renderer/index.md) : [ResponseRenderer](../../../../packages/graalvm/elide.server/-response-renderer/index.md)&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; <br>Describes supported server-renderer API methods, which are used by the framework to translate result content from embedded SSR scripts. |
| [ServerSSRRenderer](-server-s-s-r-renderer/index.md) | [jvm]<br>class [ServerSSRRenderer](-server-s-s-r-renderer/index.md)(body: [BODY](../../../../packages/server/kotlinx.html/-b-o-d-y/index.md), handler: [ElideController](../../../../packages/server/server/elide.server.controller/-elide-controller/index.md), request: HttpRequest&lt;*&gt;, script: [JsRuntime.ExecutableScript](../elide.runtime.graalvm/-js-runtime/-executable-script/index.md)) : [SuspensionRenderer](../../../../packages/graalvm/elide.server/-suspension-renderer/index.md)&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; <br>Renderer class which executes JavaScript via SSR and provides the resulting response to Micronaut. |
