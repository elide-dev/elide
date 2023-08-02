import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get

/** Build tools for the build tools. */
object ElideSubstrate {
  // Substrate Kotlin API version.
  const val API_VERSION = "1.9"

  // Substrate Kotlin language version.
  const val KOTLIN_VERSION = "1.9"

  // Publishing: Repositories
  // ------------------------
  // Configure publish targets for artifacts.
  fun PublishingExtension.elideTarget(
    project: Project,
    label: String,
    group: String,
    artifact: String,
    summary: String,
    parent: Boolean = false,
    bom: Boolean = false,
  ) {
    publications.create("maven", MavenPublication::class.java) {
      artifactId = artifact
      groupId = group

      pom {
        name.set(label)
        artifactId = artifact
        groupId = group

        url.set("https://github.com/elide-dev/v3")
        description.set(summary)
        if (!parent && !bom) from(project.components.get("kotlin"))
        else if (bom) from(project.components.get("javaPlatform"))
        else if (parent) from(project.components.get("kotlin"))

        licenses {
          license {
            name.set("MIT License")
            url.set("https://github.com/elide-dev/elide/blob/v3/LICENSE")
          }
        }
        developers {
          developer {
            id.set("sgammon")
            name.set("Sam Gammon")
            email.set("samuel.gammon@gmail.com")
          }
        }
        scm {
          url.set("https://github.com/elide-dev/v3")
        }
      }
    }
  }
}
