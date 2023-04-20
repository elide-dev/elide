
plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  maven("https://maven.pkg.st/")
  gradlePluginPortal()
  maven("https://elide-snapshots.storage-download.googleapis.com/repository/v3/")
}

val kotlinVersion = "1.8.20"

dependencies {
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.ksp)
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
