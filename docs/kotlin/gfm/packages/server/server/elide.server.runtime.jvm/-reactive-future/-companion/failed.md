//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[Companion](index.md)/[failed](failed.md)

# failed

[jvm]\
fun &lt;[R](failed.md)&gt; [failed](failed.md)(error: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)): [ReactiveFuture](../index.md)&lt;[R](failed.md)&gt;

Create an already-failed future, wrapping the provided exception instance. The future will present as one as soon as it is returned from this method.

Calling [Future.get](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html#get--) or [Future.get](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html#get--) on a failed future will surface the associated exception where invocation occurs. Under the hood, this is simply a [ReactiveFuture](../index.md) wrapping a call to Futures.immediateFailedFuture.

#### Return

Reactive future wrapping a finished value.

## Parameters

jvm

| | |
|---|---|
| error | Error to wrap in an already-failed future. |
| R | Return value generic type. |
