//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[Companion](index.md)/[done](done.md)

# done

[jvm]\
fun &lt;[R](done.md)&gt; [done](done.md)(value: [R](done.md)): [ReactiveFuture](../index.md)&lt;[R](done.md)&gt;

Create an already-resolved future, wrapping the provided value. The future will present as done as soon as it is returned from this method.

Under the hood, this is simply a [ReactiveFuture](../index.md) wrapping a call to Futures.immediateFuture.

#### Return

Reactive future wrapping a finished value.

## Parameters

jvm

| | |
|---|---|
| value | Value to wrap in an already-completed future. |
| R | Return value generic type. |
