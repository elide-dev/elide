package elide.annotations.core

/**
 * # Annotation: Experimental
 *
 * Marks a type or member as experimental. Experimental types and members may generate compile warnings if used without
 * opt-in flags.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.FIELD,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.TYPEALIAS,
  AnnotationTarget.FILE,
)
public annotation class Experimental
