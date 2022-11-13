@file:Suppress(
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js")
}

group = "dev.elide"
version = rootProject.version as String

val kotlinLanguageVersion = project.properties["versions.kotlin.language"] as String
val ecmaVersion = project.properties["versions.ecma.language"] as String


// Compiler: Kotlin
// ----------------
// Configure Kotlin compiler.
kotlin {
  explicitApi()

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
        artifactId = project.name
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

dependencies {
  implementation(kotlin("stdlib-js"))
  implementation(project(":packages:base"))

  implementation(libs.kotlinx.coroutines.core.js)
  implementation(libs.kotlinx.serialization.core.js)
  implementation(libs.kotlinx.serialization.json.js)
  implementation(libs.kotlinx.serialization.protobuf.js)

  // Testing
  testImplementation(project(":packages:test"))
}
