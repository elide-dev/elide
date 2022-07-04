package dev.elide.buildtools.gradle.plugin.tasks

import com.google.protobuf.Timestamp
import dev.elide.buildtools.gradle.plugin.ElideExtension
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import tools.elide.assets.AssetBundle
import tools.elide.assets.assetBundle
import java.time.Instant

/** Task which creates Elide asset specifications for embedding in app JARs. */
@Suppress("UnusedPrivateMember")
abstract class BundleAssetsBuildTask : BundleSpecTask<AssetBundle, AssetBundleSpec>() {
    companion object {
        private const val TASK_NAME = "bundleAssets"

        @JvmStatic fun isEligible(project: Project): Boolean {
            return project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
        }

        @JvmStatic fun install(extension: ElideExtension, project: Project) {
            project.plugins.withId("org.jetbrains.kotlin.jvm") {
                // we're applying to a JVM Kotlin target. in this case, we're consuming static assets from other
                // modules  or from within the resource section of this module.
                val processResources = project.tasks.named(
                    "processResources",
                )

                // register task to build embedded asset spec
                project.tasks.register(TASK_NAME, BundleAssetsBuildTask::class.java) {
                    it.dependsOn(processResources)
                }
            }
        }
    }

    init {
        description = "Configures an application target for use with ESBuild or Webpack"
        group = BasePlugin.BUILD_GROUP

        // set defaults
        with(project) {
            // setup asset spec
            outputSpecName.set(
                StaticValues.defaultEncoding.fileNamed("app")
            )
            bundleEncoding.set(
                StaticValues.defaultEncoding
            )
        }
    }

    /** @inheritDoc */
    override fun buildAssetCatalog(builderOp: AssetBundleSpec.() -> Unit): AssetBundle {
        val bundle = assetBundle(builderOp).toBuilder().apply {
            version = StaticValues.currentVersion
            generated = Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build()
        }.build()

        val digest = fingerprintMessage(
            bundle,
        )
        return if (digest != null) {
            bundle.toBuilder().setDigest(digest).build()
        } else {
            bundle
        }
    }

    /** Whether to compress assets. */
    @get:Input
    @get:Option(
        option = "compress",
        description = "Whether to pre-compress assets before adding them to the embedded catalog.",
    )
    var compress: Boolean = true

    /** Whether to inline assets. */
    @get:Input
    @get:Option(
        option = "inline",
        description = "Whether to inline assets directly into the catalog, or instead prefer multi-file loading.",
    )
    var inline: Boolean = true

    /** @inheritDoc */
    override fun assetCatalog(): AssetBundleSpec.() -> Unit = {
        // nothing yet
    }

    /** @inheritDoc */
    override fun runAction() {
        // we only support inline assets at this time
        if (!inline) throw NotImplementedError("Multi-file asset loading is not yet implemented.")
    }
}
