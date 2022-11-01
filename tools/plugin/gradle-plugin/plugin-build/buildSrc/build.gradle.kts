val kotlinVersion = "1.7.20-RC"

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    api(kotlin("gradle-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
