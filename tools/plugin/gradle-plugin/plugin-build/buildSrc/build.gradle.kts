@file:Suppress(
    "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.7.21"

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api(kotlin("gradle-plugin"))
    api(libs.plugin.kotlin.allopen)
    api(libs.plugin.kotlin.noarg)
    implementation(libs.plugin.kotlinx.serialization)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
