
plugins {
  idea
  kotlin("js")
  kotlin("kapt")
  kotlin("plugin.atomicfu")
  kotlin("plugin.serialization")
}

kotlin {
  js {
    nodejs()
  }
}

dependencies {
  api(npm("esbuild", Versions.esbuild))
  api(npm("esbuild-plugin-alias", Versions.esbuildPluginAlias))
  api(npm("buffer", Versions.nodeBuffers))
  api(npm("readable-stream", Versions.nodeStreams))
  implementation(project(":graalvm-js"))
  implementation("org.jetbrains.kotlinx:kotlinx-nodejs:${Versions.nodeDeclarations}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-node:${Versions.node}-${Versions.kotlinWrappers}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react:${Versions.react}-${Versions.kotlinWrappers}")
  implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:${Versions.react}-${Versions.kotlinWrappers}")
}
