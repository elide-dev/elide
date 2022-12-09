@file:Suppress(
    "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.8.0-RC"

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
