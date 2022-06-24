//[base](../../../index.md)/[elide.annotations](../index.md)/[Logic](index.md)

# Logic

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.TYPE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-t-y-p-e/index.html)])

expect annotation class [Logic](index.md)

Marks an application class as &quot;business logic,&quot; which automatically makes it eligible for dependency injection, autowired logging, and other framework features.

This annotation should be used on the <i>implementation</i> of a given Java or Kotlin interface. API interfaces should be marked with {@link API} to participate in auto-documentation and other AOT-based features.

[js, native]\
actual annotation class [Logic](index.md)

Marks an application class as &quot;business logic,&quot; which automatically makes it eligible for dependency injection, auto- wired logging, and other framework features.

This annotation should be used on the <i>implementation</i> of a given Java or Kotlin interface. API interfaces should be marked with {@link API} to participate in auto-documentation and other AOT-based features.

[jvm]\
@Singleton

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.TYPE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-t-y-p-e/index.html)])

actual annotation class [Logic](index.md)

Marks an application class as &quot;business logic,&quot; which automatically makes it eligible for dependency injection, auto- wired logging, and other framework features.

This annotation should be used on the <i>implementation</i> of a given Java or Kotlin interface. API interfaces should be marked with {@link API} to participate in auto-documentation and other AOT-based features.
