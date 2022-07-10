//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)/[isDone](is-done.md)

# isDone

[jvm]\
open override fun [isDone](is-done.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns `true` if this task completed. This defers to the underlying future, or a wrapped object if using a Reactive Java Publisher.

Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method will return `true`.

#### Return

`true` if this task completed.
