//[server](../../index.md)/[elide.server.annotations](index.md)

# Package elide.server.annotations

## Types

| Name | Summary |
|---|---|
| [Eager](-eager/index.md) | [jvm]<br>@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-c-l-a-s-s/index.html), [AnnotationTarget.ANNOTATION_CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-a-n-n-o-t-a-t-i-o-n_-c-l-a-s-s/index.html)])<br>annotation class [Eager](-eager/index.md)<br>Triggers eager initialization on the target class or type. |
| [Page](-page/index.md) | [jvm]<br>@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-c-l-a-s-s/index.html)])<br>@Bean<br>@Controller<br>@DefaultScope(value = Singleton::class)<br>annotation class [Page](-page/index.md)(val route: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = UriMapping.DEFAULT_URI, val produces: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = [MediaType.TEXT_HTML], val consumes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = [MediaType.TEXT_HTML])<br>Annotations which is applied to handlers which respond to HTTP requests with HTTP responses, typically encoded in HTML, for the purpose of presenting a user interface. |
