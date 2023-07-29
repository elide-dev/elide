@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package elide.ssr.annotations

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import kotlinx.serialization.MetaSerializable

/**
 * TBD
 */
@Introspected
@ReflectiveAccess
@Target(AnnotationTarget.CLASS)
@MetaSerializable
public actual annotation class Props
