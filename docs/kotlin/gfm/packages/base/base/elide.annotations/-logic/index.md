//[base](../../../index.md)/[elide.annotations](../index.md)/[Logic](index.md)

# Logic

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.TYPE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-t-y-p-e/index.html)])

annotation class [Logic](index.md)

Marks an application class as &quot;business logic,&quot; which automatically makes it eligible for dependency injection, autowired logging, and other framework features.

This annotation should be used on the *implementation* of a given interface. API interfaces should be marked with [API](../-a-p-i/index.md) to participate in auto-documentation and other AOT-based features.
