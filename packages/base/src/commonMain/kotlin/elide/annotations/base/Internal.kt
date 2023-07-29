package elide.annotations.base

/**
 * # Annotation: Internal
 *
 * Marks a type or member as internal. Internal types and members may generate compile warnings if used outside the
 * compile unit where they are defined.
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
public annotation class Internal
