
plugins {
  id("dev.elide.build")
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  maven("https://maven.pkg.st/")
  maven("https://gradle.pkg.st/")
  maven("https://elide.pkg.st/")
}

val kotlinVersion = "1.8.21"

dependencies {
  implementation(libs.plugin.kotlin.allopen)
  implementation(libs.plugin.kotlin.noarg)
  implementation(libs.plugin.kotlinx.serialization)
  implementation(libs.plugin.ksp)
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
