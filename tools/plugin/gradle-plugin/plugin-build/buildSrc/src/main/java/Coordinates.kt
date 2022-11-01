object PluginCoordinates {
    const val ID = "dev.elide.buildtools.plugin"
    const val GROUP = "dev.elide.buildtools"
    const val VERSION = "1.0.0-beta8"
    const val IMPLEMENTATION_CLASS = "dev.elide.buildtools.gradle.plugin.ElidePlugin"
}

object PluginBundle {
    const val VCS = "https://github.com/elide-dev/buildtools"
    const val WEBSITE = "https://github.com/elide-dev/buildtools"
    const val DESCRIPTION = "Plugin for building Elide apps in Kotlin"
    const val DISPLAY_NAME = "Elide Plugin for Gradle"
    val TAGS = listOf(
        "multiplatform",
        "apps",
        "elide",
        "micronaut",
        "react",
        "graalvm",
        "ssr",
    )
}
