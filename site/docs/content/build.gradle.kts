
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
        implementation(kotlin("stdlib-common"))
        implementation(libs.kotlinx.serialization.core)
      }
    }
    val commonTest by getting
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
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
