//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[DefaultExecutor](index.md)

# DefaultExecutor

[jvm]\
@Context

@Singleton

class [DefaultExecutor](index.md)@Injectconstructor(uncaughtHandler: [Thread.UncaughtExceptionHandler](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.UncaughtExceptionHandler.html)) : [AppExecutor](../index.md)

Implements the application-default-executor, as a bridge to Micronaut.

## Constructors

| | |
|---|---|
| [DefaultExecutor](-default-executor.md) | [jvm]<br>@Inject<br>fun [DefaultExecutor](-default-executor.md)(uncaughtHandler: [Thread.UncaughtExceptionHandler](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.UncaughtExceptionHandler.html)) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [executor](../executor.md) | [jvm]<br>open fun [executor](../executor.md)(): [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html) |
| [overrideExecutor](override-executor.md) | [jvm]<br>fun [overrideExecutor](override-executor.md)(exec: ListeningScheduledExecutorService)<br>Override the active main application executor with the provided [exec](override-executor.md) service. |
| [service](service.md) | [jvm]<br>open override fun [service](service.md)(): ListeningScheduledExecutorService |
