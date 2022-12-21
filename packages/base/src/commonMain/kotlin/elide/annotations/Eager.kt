package elide.annotations

/**
 * Triggers eager initialization on the target class or type.
 *
 * Targets which are eagerly initialized are instantiated early in the server startup routine.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
public annotation class Eager
