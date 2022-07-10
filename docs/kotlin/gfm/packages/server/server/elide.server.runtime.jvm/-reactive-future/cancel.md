//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)/[cancel](cancel.md)

# cancel

[jvm]\
open override fun [cancel](cancel.md)(mayInterruptIfRunning: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Attempts to cancel execution of this task.  This attempt will fail if the task has already completed, has already been cancelled, or could not be cancelled for some other reason. If successful, and this task has not started when `cancel` is called, this task should never run.  If the task has already started, then the `mayInterruptIfRunning` parameter determines whether the thread executing this task should be interrupted in an attempt to stop the task.

After this method returns, subsequent calls to .isDone will always return `true`.  Subsequent calls to .isCancelled will always return `true` if this method returned `true`.

#### Return

`false` if the task could not be cancelled, typically because it has already completed normally; `true` otherwise.

## Parameters

jvm

| | |
|---|---|
| mayInterruptIfRunning | `true` if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete |
