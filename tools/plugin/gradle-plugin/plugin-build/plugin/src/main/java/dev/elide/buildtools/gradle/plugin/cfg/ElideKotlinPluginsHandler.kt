package dev.elide.buildtools.gradle.plugin.cfg

import org.gradle.api.model.ObjectFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Handles configuration of Kotlin plugins provided by Elide. */
public open class ElideKotlinPluginsHandler @Inject constructor(objects: ObjectFactory) {
    /** Configuration for the Redakt plugin for Kotlin. */
    public val redaktOptions: PluginHandler.RedaktHandler = objects.newInstance(PluginHandler.RedaktHandler::class.java)

    /** Configuration block for the Redakt plugin. */
    public fun redakt(action: PluginHandler.RedaktHandler.() -> Unit) {
        action(redaktOptions)
    }

    /** Abstract base for plugin configuration blocks. */
    public sealed class PluginHandler constructor (
        /** Plugin name. */
        internal val name: String
    ) {
        /** Whether this plugin is enabled. */
        internal val enabled: AtomicBoolean = AtomicBoolean(true)

        /** Enable a Kotlin plugin. */
        public fun enable() {
            enabled.set(true)
        }

        /** Disable a Kotlin plugin. */
        public fun disable() {
            enabled.set(false)
        }

        /** Configuration for the Redakt plugin. */
        public open class RedaktHandler : PluginHandler(name = "redakt") {
            // Mask parameter for the Redakt plugin.
            internal val mask: AtomicReference<String?> = AtomicReference(null)

            // Annotation parameter for the Redakt plugin.
            internal val annotation: AtomicReference<String?> = AtomicReference(null)

            /** Set the mask [value] to use when redacting data. */
            public fun mask(value: String) {
                mask.set(value)
            }

            /** Fully-qualified path to the annotation to scan for. */
            public fun annotation(target: String) {
                annotation.set(target)
            }
        }
    }
}
