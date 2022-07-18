//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[UncaughtExceptionHandler](index.md)

# UncaughtExceptionHandler

[jvm]\
@Singleton

open class [UncaughtExceptionHandler](index.md) : [Thread.UncaughtExceptionHandler](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.UncaughtExceptionHandler.html)

Default uncaught exception handler; logs the error to the root logger, along with a stacktrace and message from the exception, if any.

Application-level code can override this default handler by using the `@Replaces` annotation from Micronaut, as demonstrated below:

```kotlin
@Singleton @Replaces(UncaughtExceptionHandler::class)
class MyHandler: Thread.UncaughtExceptionHandler {
  // ...
}
```

## Constructors

| | |
|---|---|
| [UncaughtExceptionHandler](-uncaught-exception-handler.md) | [jvm]<br>fun [UncaughtExceptionHandler](-uncaught-exception-handler.md)() |

## Functions

| Name | Summary |
|---|---|
| [uncaughtException](uncaught-exception.md) | [jvm]<br>open override fun [uncaughtException](uncaught-exception.md)(thread: [Thread](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.html), err: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |
