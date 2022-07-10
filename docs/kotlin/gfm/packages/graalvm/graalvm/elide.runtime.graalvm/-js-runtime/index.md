//[graalvm](../../../index.md)/[elide.runtime.graalvm](../index.md)/[JsRuntime](index.md)

# JsRuntime

[jvm]\
@Context

class [JsRuntime](index.md)

JavaScript embedded runtime logic, for use on the JVM.

## Constructors

| | |
|---|---|
| [JsRuntime](-js-runtime.md) | [jvm]<br>fun [JsRuntime](-js-runtime.md)() |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |
| [EmbeddedScript](-embedded-script/index.md) | [jvm]<br>class [EmbeddedScript](-embedded-script/index.md)(val path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), charset: [Charset](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html) = StandardCharsets.UTF_8, invocationBase: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, invocationTarget: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null) : [JsRuntime.ExecutableScript](-executable-script/index.md)<br>Embedded script implementation which pulls from local JAR resources. |
| [ExecutableScript](-executable-script/index.md) | [jvm]<br>sealed class [ExecutableScript](-executable-script/index.md)<br>Embedded script descriptor object. |
| [LiteralScript](-literal-script/index.md) | [jvm]<br>class [LiteralScript](-literal-script/index.md)(moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), script: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) : [JsRuntime.ExecutableScript](-executable-script/index.md)<br>Embedded script implementation which pulls from a string literal. |
| [Script](-script/index.md) | [jvm]<br>object [Script](-script/index.md)<br>Shortcuts for creating script descriptors. |

## Functions

| Name | Summary |
|---|---|
| [execute](execute.md) | [jvm]<br>suspend fun &lt;[R](execute.md)&gt; [execute](execute.md)(script: [JsRuntime.ExecutableScript](-executable-script/index.md), returnType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](execute.md)&gt;, vararg arguments: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [R](execute.md)?<br>Suspension execution of the provided [script](execute.md) within an embedded JavaScript VM, by way of GraalVM's runtime engine; de-serialize the result [R](execute.md) and provide it as the return value. |
| [executeAsync](execute-async.md) | [jvm]<br>fun &lt;[R](execute-async.md)&gt; [executeAsync](execute-async.md)(script: [JsRuntime.ExecutableScript](-executable-script/index.md), returnType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](execute-async.md)&gt;, vararg arguments: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): Deferred&lt;[R](execute-async.md)?&gt;<br>Asynchronously execute the provided [script](execute-async.md) within an embedded JavaScript VM, by way of GraalVM's runtime engine; de-serialize the result [R](execute-async.md) and provide it as the return value. |
| [executeBlocking](execute-blocking.md) | [jvm]<br>fun &lt;[R](execute-blocking.md)&gt; [executeBlocking](execute-blocking.md)(script: [JsRuntime.ExecutableScript](-executable-script/index.md), returnType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](execute-blocking.md)&gt;, vararg arguments: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [R](execute-blocking.md)?<br>Blocking execution of the provided [script](execute-blocking.md) within an embedded JavaScript VM, by way of GraalVM's runtime engine; de-serialize the result [R](execute-blocking.md) and provide it as the return value. |
