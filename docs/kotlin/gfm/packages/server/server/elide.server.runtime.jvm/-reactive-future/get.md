//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)/[get](get.md)

# get

[jvm]\
open override fun [get](get.md)(): [R](index.md)

Waits if necessary for the computation to complete, and then retrieves its result.

It is generally recommended to use the variant of this method which specifies a timeout - one must handle the additional [TimeoutException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html), but on the other hand the computation can never infinitely block if an async value does not materialize.

#### Return

the computed result.

## See also

jvm

| | |
|---|---|
|  | .get |

## Throws

| | |
|---|---|
| [java.util.concurrent.CancellationException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CancellationException.html) | if the computation was cancelled |
| [java.util.concurrent.ExecutionException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutionException.html) | if the computation threw an exception |
| [java.lang.InterruptedException](https://docs.oracle.com/javase/8/docs/api/java/lang/InterruptedException.html) | if the current thread was interrupted while waiting |

[jvm]\
open operator override fun [get](get.md)(timeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), unit: [TimeUnit](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html)): [R](index.md)

Waits if necessary for at most the given time for the computation to complete, and then retrieves its result, if available.

#### Return

the computed result

## Parameters

jvm

| | |
|---|---|
| timeout | the maximum time to wait |
| unit | the time unit of the timeout argument |

## Throws

| | |
|---|---|
| [java.util.concurrent.CancellationException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CancellationException.html) | if the computation was cancelled |
| [java.util.concurrent.ExecutionException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutionException.html) | if the computation threw an exception |
| [java.lang.InterruptedException](https://docs.oracle.com/javase/8/docs/api/java/lang/InterruptedException.html) | if the current thread was interrupted while waiting |
| [java.util.concurrent.TimeoutException](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html) | if the wait timed out |
