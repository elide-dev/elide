@file:Suppress(
  "UNUSED_VARIABLE",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  id("dev.elide.build.js")
}

group = "dev.elide"
version = rootProject.version as String

kotlin {
  explicitApi()

  sourceSets {
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
        implementation(project(":packages:base"))

        implementation(libs.kotlinx.coroutines.core.js)
        implementation(libs.kotlinx.serialization.core.js)
        implementation(libs.kotlinx.serialization.json.js)
        implementation(libs.kotlinx.serialization.protobuf.js)
      }
    }

    val jsTest by getting {
      dependencies {
        implementation(project(":packages:test"))
      }
    }
  }
}

val buildDocs = project.properties["buildDocs"] == "true"
val javadocJar: TaskProvider<Jar>? = if (buildDocs) {
  val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

  val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier = "javadoc"
    from(dokkaHtml.outputDirectory)
  }
  javadocJar
} else null

publishing {
  publications.withType<MavenPublication> {
    if (buildDocs) {
      artifact(javadocJar)
    }
    artifactId = artifactId.replace("frontend", "elide-frontend")

    pom {
      name = "Elide Model"
      url = "https://elide.dev"
      description = (
        "Tools for building UI experiences on top of the Elide Framework/Runtime"
        )

      licenses {
        license {
          name = "MIT License"
          url = "https://github.com/elide-dev/elide/blob/v3/LICENSE"
        }
      }
      developers {
        developer {
          id = "sgammon"
          name = "Sam Gammon"
          email = "samuel.gammon@gmail.com"
        }
      }
      scm {
        url = "https://github.com/elide-dev/elide"
      }
    }
  }
}
