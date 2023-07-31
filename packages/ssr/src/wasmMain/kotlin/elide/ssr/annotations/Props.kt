@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package elide.ssr.annotations

import kotlinx.serialization.MetaSerializable

/**
 * TBD
 */
@Target(AnnotationTarget.CLASS)
@MetaSerializable
public actual annotation class Props
