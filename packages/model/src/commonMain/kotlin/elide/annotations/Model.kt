package elide.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

/**
 * Marks an application-level class as a data model, which makes it eligible for reflective use (even in native
 * circumstances such as on GraalVM).
 *
 * Classes marked as models become available for reflection for all constructors, fields, and methods. Models should
 * typically only depend on other models (ideally via encapsulation), and should be immutable. Kotlin data classes are
 * an example of good model semantics.
 */
@MetaState
@ExperimentalSerializationApi
@MetaSerializable
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
public annotation class Model
