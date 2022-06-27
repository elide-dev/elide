//[server](../../../index.md)/[elide.server.runtime](../index.md)/[AppExecutor](index.md)

# AppExecutor

[jvm]\
interface [AppExecutor](index.md)

## Types

| Name | Summary |
|---|---|
| [DefaultExecutor](-default-executor/index.md) | [jvm]<br>@Context<br>@Singleton<br>class [DefaultExecutor](-default-executor/index.md)@Injectconstructor(uncaughtHandler: [Thread.UncaughtExceptionHandler](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.UncaughtExceptionHandler.html)) : [AppExecutor](index.md)<br>Implements the application-default-executor, as a bridge to Micronaut. |
| [DefaultSettings](-default-settings/index.md) | [jvm]<br>object [DefaultSettings](-default-settings/index.md) |

## Functions

| Name | Summary |
|---|---|
| [executor](executor.md) | [jvm]<br>open fun [executor](executor.md)(): [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html) |
| [service](service.md) | [jvm]<br>abstract fun [service](service.md)(): ListeningScheduledExecutorService |

## Inheritors

| Name |
|---|
| [DefaultExecutor](-default-executor/index.md) |
