//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[Companion](index.md)/[wrap](wrap.md)

# wrap

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(publisher: Publisher&lt;[R](wrap.md)&gt;?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a Reactive Java Publisher in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring a supported async or future value.

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| publisher | Reactive publisher to wrap. |
| R | Return or emission type of the publisher. |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If the passed `publisher` is `null`. |

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(future: [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)&lt;[R](wrap.md)&gt;?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a regular Java [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring support for that class.

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

**Warning:** this method uses MoreExecutors.directExecutor for callback execution. You should only do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily recommended to use the variants of `wrap` that accept an [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html). For instance, the corresponding method to this one is .wrap.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| future | Completable future to wrap. |
| R | Return or emission type of the future. |

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(future: [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)&lt;[R](wrap.md)&gt;?, executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html)?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a regular Java [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring support for that class.

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| future | Completable future to wrap. |
| executor | Executor to use. |
| R | Return or emission type of the future. |

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(future: ListenableFuture&lt;[R](wrap.md)&gt;?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a Guava ListenableFuture in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring a supported async or future value.

**Warning:** this method uses MoreExecutors.directExecutor for callback execution. You should only do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily recommended to use the variants of `wrap` that accept an [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html). For instance, the corresponding method to this one is .wrap.

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| future | Future value to wrap. |
| R | Return value type for the future. |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If the passed `future` is `null`. |

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(future: ListenableFuture&lt;[R](wrap.md)&gt;?, executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html)?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a Guava ListenableFuture in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring a supported async or future value.

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| future | Future value to wrap. |
| executor | Executor to dispatch callbacks with. |
| R | Return value type for the future. |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If the passed `future` is `null`. |

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(apiFuture: ApiFuture&lt;[R](wrap.md)&gt;?, executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html)?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a Google APIs ApiFuture in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring a supported async or future value.

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| apiFuture | API future to wrap. |
| executor | Executor to run callbacks with. |
| R | Return value type for the future. |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If the passed `apiFuture` is `null`. |

[jvm]\
fun &lt;[R](wrap.md)&gt; [wrap](wrap.md)(apiFuture: ApiFuture&lt;[R](wrap.md)&gt;?): [ReactiveFuture](../index.md)&lt;[R](wrap.md)&gt;

Wrap a Google APIs ApiFuture in a universal [ReactiveFuture](../index.md), such that it may be used with any interface requiring a supported async or future value.

**Warning:** this method uses MoreExecutors.directExecutor for callback execution. You should only do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily recommended to use the variants of `wrap` that accept an [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html). For instance, the corresponding method to this one is [wrap](wrap.md).

The resulting object is usable as any of ListenableFuture, Publisher, or ApiFuture. See class docs for more information.

**Note:** to use a Publisher as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) (or any descendent thereof), the Publisher may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class and accessed as a [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html), to prevent silently dropping intermediate values on the floor.

#### Return

Wrapped reactive future object.

## Parameters

jvm

| | |
|---|---|
| apiFuture | API future to wrap. |
| R | Return value type for the future. |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If the passed `apiFuture` is `null`. |
