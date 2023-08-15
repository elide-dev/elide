package elide.runtime.core

/**
 * Marks an element as part of Elide's delicate core API, which is intended to be used by the runtime development team.
 *
 * Note that this marker is meant to prevent accidental use of core APIs, there is no inherent issue with using
 * annotated symbols, unless otherwise stated in their specific documentation.
 */
@Target(
  AnnotationTarget.CLASS,
  AnnotationTarget.PROPERTY,
  AnnotationTarget.FIELD,
  AnnotationTarget.LOCAL_VARIABLE,
  AnnotationTarget.VALUE_PARAMETER,
  AnnotationTarget.CONSTRUCTOR,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
@RequiresOptIn("This symbol is part of Elide's core API, and should not be used in general code.")
public annotation class DelicateElideApi