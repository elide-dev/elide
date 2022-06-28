@file:Suppress("unused")

package dev.elide.buildtools.gradle.plugin.tasks

import java.io.File

internal fun String.fixSlashes() =
    replace("\\", "\\\\")

internal val EmbeddedJsBuildTask.outputBundleFile
    get() = File(outputBundleFolder.get(), outputBundleName.get())

internal fun StringBuilder.appendLine(element: String = ""): StringBuilder =
    append(element).append("\n")
