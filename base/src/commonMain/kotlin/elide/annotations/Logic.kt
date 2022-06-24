package elide.annotations


/**
 * Marks an application class as "business logic," which automatically makes it eligible for dependency injection,
 * autowired logging, and other framework features.
 *
 * This annotation should be used on the <i>implementation</i> of a given Java or Kotlin interface. API interfaces
 * should be marked with {@link API} to participate in auto-documentation and other AOT-based features.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
expect annotation class Logic
