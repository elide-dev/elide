package elide.annotations


/**
 * # Annotation: Logic
 *
 * Marks an application class as "business logic," which automatically makes it eligible for dependency injection,
 * autowired logging, and other framework features.
 *
 * This annotation should be used on the *implementation* of a given interface. API interfaces should be marked with
 * [API] to participate in auto-documentation and other AOT-based features.
 *
 * @see API corresponding annotation for API interfaces.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
public annotation class Logic
