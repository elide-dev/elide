//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[CompletableFuturePublisher](index.md)/[thenCombineAsync](then-combine-async.md)

# thenCombineAsync

[jvm]\
open override fun &lt;[U](then-combine-async.md), [V](then-combine-async.md)&gt; [thenCombineAsync](then-combine-async.md)(other: [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;out [U](then-combine-async.md)?&gt;, fn: [BiFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/BiFunction.html)&lt;in [T](index.md), in [U](then-combine-async.md)?, out [V](then-combine-async.md)&gt;): [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;[V](then-combine-async.md)&gt;

open override fun &lt;[U](then-combine-async.md), [V](then-combine-async.md)&gt; [thenCombineAsync](then-combine-async.md)(other: [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;out [U](then-combine-async.md)?&gt;, fn: [BiFunction](https://docs.oracle.com/javase/8/docs/api/java/util/function/BiFunction.html)&lt;in [T](index.md), in [U](then-combine-async.md)?, out [V](then-combine-async.md)&gt;, executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html)): [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;[V](then-combine-async.md)&gt;
