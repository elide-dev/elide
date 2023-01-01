@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "DSL_SCOPE_VIOLATION",
)

import dev.elide.buildtools.gradle.plugin.BuildMode
import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm

plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.aot)

    kotlin("jvm")
//    alias(libs.plugins.ksp)
    id("dev.elide.buildtools.plugin")
}

application {
    mainClass.set("example.App")
}

dependencies {
//    ksp(libs.elide.tools.processor)
//    ksp(libs.autoService.ksp)

    implementation(libs.elide.base)
    implementation(libs.elide.server)
    implementation(libs.elide.proto.core)
    implementation(libs.elide.proto.protobuf)
    implementation(libs.google.auto.service.annotations)

    implementation("io.micronaut:micronaut-context")
    implementation("io.micronaut:micronaut-runtime")
}

micronaut {
    version.set("3.7.5")
}

elide {
    mode = BuildMode.DEVELOPMENT

    server {
//        ssg {
//            enable()
//        }

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

tasks.named("nativeCompile").configure {
    onlyIf { false }
}
