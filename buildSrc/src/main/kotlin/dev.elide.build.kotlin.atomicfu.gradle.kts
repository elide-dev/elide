plugins {
  id("kotlinx-atomicfu")
}

val enableAtomicFu = project.properties["elide.atomicFu"] as String == "true"

if (enableAtomicFu) {
  atomicfu {
    transformJs = true
    transformJvm = true
    jvmVariant = "VH"  // `VarHandle`
  }
}
