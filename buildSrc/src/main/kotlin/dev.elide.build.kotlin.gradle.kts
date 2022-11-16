plugins {
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  id("org.jetbrains.kotlinx.kover")
  id("kotlinx-atomicfu")
}

atomicfu {
  transformJs = true
  transformJvm = true
  jvmVariant = "VH"  // `VarHandle`
}
