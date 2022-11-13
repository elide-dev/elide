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
  explicitApi()

  js {
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
        artifactId = "graalvm-js"
        version = rootProject.version as String

        from(components["kotlin"])
      }
    }
  }
}

dependencies {
  api(npm("esbuild", libs.versions.npm.esbuild.get()))
  api(npm("prepack", libs.versions.npm.prepack.get()))
  api(npm("buffer", libs.versions.npm.buffer.get()))
  api(npm("readable-stream", libs.versions.npm.stream.get()))

  implementation(libs.kotlinx.wrappers.node)

  // Testing
  testImplementation(project(":packages:test"))
}
