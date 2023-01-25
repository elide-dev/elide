@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package elide.annotations

import kotlinx.serialization.MetaSerializable

/**
 * TBD
 */
@Target(AnnotationTarget.CLASS)
@MetaSerializable
public expect annotation class Props constructor ()
