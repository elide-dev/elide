package elide.annotations


/**
 * Marks a given Java or Kotlin class as "Generated," which excuses it from coverage requirements and other tooling
 * strictness; should be used sparingly.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@MustBeDocumented
public annotation class Generated
