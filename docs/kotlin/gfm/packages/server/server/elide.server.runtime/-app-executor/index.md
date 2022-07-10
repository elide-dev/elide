//[server](../../../index.md)/[elide.server.runtime](../index.md)/[AppExecutor](index.md)

# AppExecutor

[jvm]\
interface [AppExecutor](index.md)

Defines the interface expected for an application-level executor; there is a default implementation provided by the framework, which uses Guava executors integrated with Kotlin Coroutines.

See more about Guava concurrent execution tools here: https://github.com/google/guava/wiki

See more about Kotlin Coroutines here: https://kotlinlang.org/docs/coroutines-overview.html

## See also

jvm

| | |
|---|---|
| [elide.server.runtime.AppExecutor.DefaultExecutor](-default-executor/index.md) | for the default [AppExecutor](index.md) implementation. |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |
| [DefaultExecutor](-default-executor/index.md) | [jvm]<br>@Context<br>@Singleton<br>class [DefaultExecutor](-default-executor/index.md)@Injectconstructor(uncaughtHandler: [Thread.UncaughtExceptionHandler](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.UncaughtExceptionHandler.html)) : [AppExecutor](index.md)<br>Implements the application-default-executor, as a bridge to Micronaut. |
| [DefaultSettings](-default-settings/index.md) | [jvm]<br>object [DefaultSettings](-default-settings/index.md)<br>Default settings applied to an application executor. |

## Functions

| Name | Summary |
|---|---|
| [executor](executor.md) | [jvm]<br>open fun [executor](executor.md)(): [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html) |
| [service](service.md) | [jvm]<br>abstract fun [service](service.md)(): ListeningExecutorService |

## Inheritors

| Name |
|---|
| [DefaultExecutor](-default-executor/index.md) |
