@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import tools.elide.assets.EmbeddedScriptLanguage
import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.elide.buildtools.plugin")
}

dependencies {
    ksp(libs.elide.tools.processor)
}

elide {
    mode = BuildMode.DEVELOPMENT

    server {
        ssg.enable()

        assets {
            bundler {
                format(ManifestFormat.BINARY)
                digestAlgorithm(HashAlgorithm.SHA256)

                tagGenerator {
                    tailSize(8)
                }

                compression {
                    minimumSizeBytes(1)
                    forceVariants()
                }
            }

            // stylesheet: `main.base`
            stylesheet("main.base") {
                sourceFile("src/main/assets/basestyles.css")
            }

            script("main.ui") {
                from(project(":example:static:frontend"))
            }
        }
    }
}
