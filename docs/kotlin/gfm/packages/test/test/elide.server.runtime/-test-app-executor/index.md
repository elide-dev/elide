//[test](../../../index.md)/[elide.server.runtime](../index.md)/[TestAppExecutor](index.md)

# TestAppExecutor

[jvm]\
@Replaces(value = [AppExecutor.DefaultExecutor::class](../../../../../packages/server/server/elide.server.runtime/-app-executor/-default-executor/index.md))

@Singleton

class [TestAppExecutor](index.md) : [AppExecutor](../../../../../packages/server/server/elide.server.runtime/-app-executor/index.md)

Provides an implementation of [AppExecutor](../../../../../packages/server/server/elide.server.runtime/-app-executor/index.md) that directly executes all tasks in the current thread.

## Constructors

| | |
|---|---|
| [TestAppExecutor](-test-app-executor.md) | [jvm]<br>fun [TestAppExecutor](-test-app-executor.md)() |

## Functions

| Name | Summary |
|---|---|
| [executor](index.md#-2092315656%2FFunctions%2F-271645073) | [jvm]<br>open fun [executor](index.md#-2092315656%2FFunctions%2F-271645073)(): [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html) |
| [service](service.md) | [jvm]<br>open override fun [service](service.md)(): ListeningScheduledExecutorService |
