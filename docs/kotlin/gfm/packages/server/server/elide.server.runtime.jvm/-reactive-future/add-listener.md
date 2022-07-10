//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)/[addListener](add-listener.md)

# addListener

[jvm]\
open override fun [addListener](add-listener.md)(listener: [Runnable](https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html), executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html))

Registers a listener to be [run](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html#execute-java.lang.Runnable-) on the given executor. The listener will run when the `Future`'s computation is [complete](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html#isDone--) or, if the computation is already complete, immediately.

There is no guaranteed ordering of execution of listeners, but any listener added through this method is guaranteed to be called once the computation is complete.

Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown during `Executor.execute` (e.g., a `RejectedExecutionException` or an exception thrown by direct execution) will be caught and logged.

Note: For fast, lightweight listeners that would be safe to execute in any thread, consider MoreExecutors.directExecutor. Otherwise, avoid it. Heavyweight `directExecutor` listeners can cause problems, and these problems can be difficult to reproduce because they depend on timing. For example:

- 
   The listener may be executed by the caller of `addListener`. That caller may be a UI thread or other latency-sensitive thread. This can harm UI responsiveness.
- 
   The listener may be executed by the thread that completes this `Future`. That thread may be an internal system thread such as an RPC network thread. Blocking that thread may stall progress of the whole system. It may even cause a deadlock.
- 
   The listener may delay other listeners, even listeners that are not themselves `directExecutor` listeners.

This is the most general listener interface. For common operations performed using listeners, see Futures. For a simplified but general listener interface, see addCallback().

Memory consistency effects: Actions in a thread prior to adding a listener [*happen-before*](https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5) its execution begins, perhaps in another thread.

Guava implementations of `ListenableFuture` promptly release references to listeners after executing them.

## Parameters

jvm

| | |
|---|---|
| listener | the listener to run when the computation is complete. |
| executor | the executor to run the listener in |

## Throws

| | |
|---|---|
| [java.util.concurrent.RejectedExecutionException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/RejectedExecutionException.html) | if we tried to execute the listener immediately but the executor rejected it. |
