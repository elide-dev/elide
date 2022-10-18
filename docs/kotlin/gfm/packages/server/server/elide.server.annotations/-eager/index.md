//[server](../../../index.md)/[elide.server.annotations](../index.md)/[Eager](index.md)

# Eager

[jvm]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-c-l-a-s-s/index.html), [AnnotationTarget.ANNOTATION_CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-a-n-n-o-t-a-t-i-o-n_-c-l-a-s-s/index.html)])

annotation class [Eager](index.md)

Triggers eager initialization on the target class or type.

Targets which are eagerly initialized are instantiated early in the server startup routine.
