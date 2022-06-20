
val protobufVersion = project.properties["versions.protobuf"] as String
val protobufTypesVersion = project.properties["versions.protobufTypes"] as String
val grpcWebVersion = project.properties["versions.grpcWeb"] as String
val kotlinxCoroutinesVersion = project.properties["versions.kotlinx.coroutines"] as String
val kotlinxSerializationVersion = project.properties["versions.kotlinx.serialization"] as String

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

dependencies {
  implementation(project(":base"))
  implementation(project(":frontend"))
  implementation(npm("@types/google-protobuf", protobufTypesVersion))
  implementation(npm("google-protobuf", protobufVersion))
  implementation(npm("grpc-web", grpcWebVersion, generateExternals = true))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinxCoroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:$kotlinxSerializationVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf-js:$kotlinxSerializationVersion")
}
