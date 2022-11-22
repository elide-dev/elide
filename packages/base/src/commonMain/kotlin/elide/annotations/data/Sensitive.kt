package elide.annotations.data

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.CLASS

/**
 *
 */
@MustBeDocumented
@Target(CLASS, PROPERTY)
@Retention(BINARY)
public annotation class Sensitive
