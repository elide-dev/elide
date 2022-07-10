//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[Companion](index.md)/[cancelled](cancelled.md)

# cancelled

[jvm]\
fun &lt;[R](cancelled.md)&gt; [cancelled](cancelled.md)(): [ReactiveFuture](../index.md)&lt;[R](cancelled.md)&gt;

Create an already-cancelled future. The future will present as both done and cancelled as soon as it is returned from this method.

Under the hood, this is simply a [ReactiveFuture](../index.md) wrapping a call to Futures.immediateCancelledFuture.

#### Return

Reactive future wrapping a cancelled operation.

## Parameters

jvm

| | |
|---|---|
| R | Return value generic type. |
