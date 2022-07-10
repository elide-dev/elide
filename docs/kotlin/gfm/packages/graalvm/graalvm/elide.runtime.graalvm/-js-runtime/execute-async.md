//[graalvm](../../../index.md)/[elide.runtime.graalvm](../index.md)/[JsRuntime](index.md)/[executeAsync](execute-async.md)

# executeAsync

[jvm]\
fun &lt;[R](execute-async.md)&gt; [executeAsync](execute-async.md)(script: [JsRuntime.ExecutableScript](-executable-script/index.md), returnType: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[R](execute-async.md)&gt;, vararg arguments: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?): Deferred&lt;[R](execute-async.md)?&gt;

Asynchronously execute the provided [script](execute-async.md) within an embedded JavaScript VM, by way of GraalVM's runtime engine; de-serialize the result [R](execute-async.md) and provide it as the return value.

#### Return

Deferred task which evaluates to the return value [R](execute-async.md) when execution finishes.

## Parameters

jvm

| | |
|---|---|
| script | Executable script spec to execute within the embedded JS VM. |
