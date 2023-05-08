import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import java.net.URI

/** Build tools for the build tools. */
object ElideSubstrate {
  // Substrate Kotlin API version.
  const val apiVersion = "1.8"

  // Substrate Kotlin language version.
  const val kotlinVerison = "1.8"

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

        url.set("https://github.com/elide-dev/elide")
        description.set(summary)
        if (!parent && !bom) from(project.components["kotlin"])
        else if (bom) from(project.components["javaPlatform"])
        else if (parent) from(project.components["kotlin"])

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
          url.set("https://github.com/elide-dev/elide")
        }
      }
    }
  }
}
