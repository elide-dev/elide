@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js")
}

group = "dev.elide"
version = rootProject.version as String

val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val ecmaVersion = project.properties["versions.ecma.language"] as String


kotlin {
  js {
    browser()
    nodejs()

    compilations.all {
      kotlinOptions {
        sourceMap = true
        moduleKind = "umd"
        metaInfo = true
        target = ecmaVersion
        apiVersion = kotlinLanguageVersion
        languageVersion = kotlinLanguageVersion
        freeCompilerArgs = Elide.jsCompilerArgs
      }
    }
  }

  publishing {
    publications {
      create<MavenPublication>("main") {
        groupId = "dev.elide"
        artifactId = "rpc-js"
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

dependencies {
  implementation(project(":packages:base"))
  implementation(project(":packages:frontend"))
  implementation(npm("@types/google-protobuf", libs.versions.npm.types.protobuf.get()))
  implementation(npm("google-protobuf", libs.versions.protobuf.get()))
  implementation(npm("grpc-web", libs.versions.npm.grpcweb.get()))

  implementation(libs.kotlinx.coroutines.core.js)
  implementation(libs.kotlinx.serialization.json.js)
  implementation(libs.kotlinx.serialization.protobuf.js)

  // Testing
  testImplementation(project(":packages:test"))
}

tasks.dokkaHtml.configure {
  moduleName.set("rpc-js")
}
