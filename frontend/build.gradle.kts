
plugins {
  idea
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
}

kotlin {
  js {
    browser()
    nodejs()
  }
}
