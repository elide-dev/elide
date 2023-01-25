
plugins {
  kotlin("plugin.noarg")
  kotlin("plugin.serialization")
  kotlin("multiplatform")
}

kotlin {
  jvm()
  js(IR) {
    browser()
  }
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(project(":packages:base"))
        implementation(project(":packages:ssr"))
        implementation(kotlin("stdlib-common"))
        implementation(libs.kotlinx.serialization.core)
        implementation(libs.kotlinx.html)
      }
    }
    val commonTest by getting
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(libs.jackson.core)
        implementation(libs.jackson.databind)
        implementation(libs.jackson.jsr310)
        implementation(libs.jackson.module.kotlin)
      }
    }
    val jvmTest by getting
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
      }
    }
    val jsTest by getting
  }
}
