//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)/[isCancelled](is-cancelled.md)

# isCancelled

[jvm]\
open override fun [isCancelled](is-cancelled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Returns `true` if this task was cancelled before it completed normally. This defers to the underlying future, or a wrapped object if using a Publisher.

#### Return

`true` if this task was cancelled before it completed
