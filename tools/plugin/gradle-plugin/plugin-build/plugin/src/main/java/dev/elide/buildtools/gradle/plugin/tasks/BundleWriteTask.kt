package dev.elide.buildtools.gradle.plugin.tasks

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Message
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import tools.elide.bundler.AssetBundler
import java.nio.charset.StandardCharsets

/** Writes a pre-built asset bundle to the specified location, in the specified format. */
abstract class BundleWriteTask : DefaultTask() {
    /** Built asset spec record to write. */
    @get:Input
    abstract val sourceTaskName: Property<String>

    /** Folder in which to put built bundle targets. */
    @get:Input
    @get:Option(
        option = "bundleEncoding",
        description = "Mode to use for encoding the asset bundle. Typically managed by the plugin.",
    )
    abstract val bundleEncoding: Property<AssetBundler.ManifestFormat>

    /** Folder in which to put built bundle targets. */
    @get:Input
    @get:Option(
        option = "outputBundleFolder",
        description = "Where to put compiled asset catalogs on the filesystem. Typically managed by the plugin.",
    )
    abstract val outputBundleFolder: Property<String>

    /** Name to give the asset catalog being affixed by this task. */
    @get:Input
    @get:Option(
        option = "outputSpecName",
        description = "Name to give the asset catalog built by this task. Typically managed by the plugin.",
    )
    abstract val outputSpecName: Property<String>

    /**
     * Write the prepared asset catalog bundle to the [outputBundleFolder], under the [outputSpecName]; this method is
     * expected to be called from the concrete task action method.
     */
    @Suppress("SENSELESS_NULL_IN_WHEN")
    @VisibleForTesting
    internal fun writeBundle(bundle: Message) {
        when (bundleEncoding.getOrElse(AssetBundler.ManifestFormat.BINARY)) {
            // for binary, use a raw byte writer
            null, AssetBundler.ManifestFormat.BINARY -> {
                val outstream = outputAssetSpecFile
                    .outputStream()
                    .buffered()

                outstream.use { buf ->
                    bundle.writeTo(buf)
                }
            }

            // for JSON or TEXT, write in UTF-8
            AssetBundler.ManifestFormat.JSON, AssetBundler.ManifestFormat.TEXT -> {
                val writer = outputAssetSpecFile
                    .outputStream()
                    .bufferedWriter(StandardCharsets.UTF_8)

                writer.use { buf ->
                    if (bundleEncoding.get() == AssetBundler.ManifestFormat.JSON) {
                        // JSON format is requested
                        buf.write(
                            BundleSpecTask.jsonPrinter.print(bundle)
                        )
                    } else {
                        // normal TEXT format is requested
                        buf.write(bundle.toString())
                    }
                }
            }
        }
    }

    @TaskAction fun writeBundle() {
        val sourceTask = project.tasks.named(sourceTaskName.get(), BundleSpecTask::class.java).get()
        val assetSpec = sourceTask.assetSpec.get()

        if (assetSpec == null) {
            throw IllegalStateException("Failed to resolve built asset spec: could not write.")
        } else {
            project.logger.lifecycle(
                "Writing asset bundle '${outputSpecName.get()}'..."
            )
            writeBundle(assetSpec)
        }
    }
}
