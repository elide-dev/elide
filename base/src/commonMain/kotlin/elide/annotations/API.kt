package elide.annotations


/**
 * Marks an application-level class as an API interface, which defines the abstract surface of a single unit of business
 * logic; combined with [Logic], classes annotated with `API` constitute a set of interface and implementation pairs.
 *
 * API should only be affixed to interfaces or abstract classes. API interface parameters are preserved and other AOT-
 * style configurations are possible based on this annotation.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
@MustBeDocumented
annotation class API
