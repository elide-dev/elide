@file:Suppress(
    "UnstableApiUsage",
    "unused",
    "UNUSED_VARIABLE",
    "DSL_SCOPE_VIOLATION",
)

plugins {
    kotlin("jvm")
    java
}

sourceSets.getByName("main").java {
    srcDir("src/generated/java")
    srcDir("src/generated/kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

kotlin {
    // Nothing at this time.
}

val javaLanguageVersion = "9"
val kotlinLanguageVersion = project.properties["kotlin.language.version"] as String
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        jvmTarget = javaLanguageVersion
        javaParameters = true
    }
}

dependencies {
    // Protocol Buffers
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.util)
    implementation(libs.protobuf.kotlin)
}
