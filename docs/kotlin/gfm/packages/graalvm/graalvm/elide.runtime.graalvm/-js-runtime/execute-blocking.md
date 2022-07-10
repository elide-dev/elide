//[graalvm](../../../index.md)/[elide.runtime.graalvm](../index.md)/[JsRuntime](index.md)/[executeBlocking](execute-blocking.md)

# executeBlocking

[jvm]\
fun &lt;[R](execute-blocking.md)&gt; [executeBlocking](execute-blocking.md)(script: [JsRuntime.ExecutableScript](-executable-script/index.md), returnType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](execute-blocking.md)&gt;, vararg arguments: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): [R](execute-blocking.md)?

Blocking execution of the provided [script](execute-blocking.md) within an embedded JavaScript VM, by way of GraalVM's runtime engine; de-serialize the result [R](execute-blocking.md) and provide it as the return value.

#### Return

Deferred task which evaluates to the return value [R](execute-blocking.md) when execution finishes.

## Parameters

jvm

| | |
|---|---|
| script | Executable script spec to execute within the embedded JS VM. |
