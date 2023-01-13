package elide.annotations.data

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * # Annotation: Sensitive
 *
 * Marks a type or member as a container of sensitive data. Sensitive data containers behave differently than normal
 * with respect to logging and other output facilities. For example, if a field marked `Sensitive` is converted to a
 * string for logging purposes, it may be redacted from view.
 */
@MustBeDocumented
@Target(CLASS, PROPERTY)
@Retention(BINARY)
public annotation class Sensitive
