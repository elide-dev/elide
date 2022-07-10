@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import tools.elide.assets.EmbeddedScriptLanguage
import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.CompressionMode

plugins {
    kotlin("jvm")
    id("dev.elide.buildtools.plugin")
}

elide {
    mode = BuildMode.DEVELOPMENT

    server {
        ssr(EmbeddedScriptLanguage.JS) {
            bundle(project(":example:fullstack:node"))
        }
        assets {
            bundler {
                format(ManifestFormat.TEXT)
                digestAlgorithm(HashAlgorithm.SHA256)

                tagGenerator {
                    tailSize(8)
                }

                compression {
                    modes(CompressionMode.GZIP)
                    minimumSizeBytes(400)
                    keepAllVariants()
                    forceVariants()
                }
            }

            // stylesheet: `main.base`
            stylesheet("main.base") {
                sourceFile("src/main/assets/basestyles.css")
            }

            // stylesheet: `main.styles`
            stylesheet("main.styles") {
                sourceFile("src/main/assets/coolstyles.css")
                dependsOn("main.base")
            }

            // script: `main.js`
            script("main.js") {
                sourceFile("src/main/assets/some-script.js")
            }

            // text: `util.humans`
            text("util.humans") {
                sourceFile("src/main/assets/humans.txt")
            }
        }
    }
}
