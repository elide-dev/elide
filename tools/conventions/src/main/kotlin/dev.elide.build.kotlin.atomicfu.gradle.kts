
val enableAtomicFu = project.properties["elide.atomicFu"] as String == "true"

if (enableAtomicFu) {
  apply(plugin = "kotlinx-atomicfu")

//  atomicfu {
//    transformJs = true
//    transformJvm = true
//    jvmVariant = "VH"  // `VarHandle`
//  }
}
