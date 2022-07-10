//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[CompletableFuturePublisher](index.md)/[handleAsync](handle-async.md)

# handleAsync

[jvm]\
open override fun &lt;[U](handle-async.md)&gt; [handleAsync](handle-async.md)(fn: [BiFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/BiFunction.html)&lt;in [T](index.md), [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html), out [U](handle-async.md)&gt;): [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;[U](handle-async.md)&gt;

open override fun &lt;[U](handle-async.md)&gt; [handleAsync](handle-async.md)(fn: [BiFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/BiFunction.html)&lt;in [T](index.md), [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html), out [U](handle-async.md)&gt;, executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html)): [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;[U](handle-async.md)&gt;
