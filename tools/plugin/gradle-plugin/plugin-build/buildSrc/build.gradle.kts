@file:Suppress(
    "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.9.20-station-823"
val javaVersion = "11"

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    api(kotlin("gradle-plugin"))
    api(libs.plugin.kotlin.allopen)
    api(libs.plugin.kotlin.noarg)
    implementation(libs.plugin.kotlinx.serialization)
    implementation(libs.elide.tools.conventions)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

afterEvaluate {
    tasks {
        compileKotlin.configure {
            kotlinOptions {
                jvmTarget = javaVersion
                javaParameters = true
            }
        }

        compileTestKotlin.configure {
            kotlinOptions {
                jvmTarget = javaVersion
                javaParameters = true
            }
        }
    }
}
