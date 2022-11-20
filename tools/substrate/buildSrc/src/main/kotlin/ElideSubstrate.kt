import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.withType
import java.net.URI

/** Build tools for the build tools. */
object ElideSubstrate {
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
  ) {
    publications.create("maven", MavenPublication::class.java) {
      pom {
        name.set(label)
        artifactId = artifact
        groupId = group
        url.set("https://github.com/elide-dev/v3")
        description.set(summary)
        if (!parent) from(project.components.get("kotlin"))
        else packaging = "pom"

        licenses {
          license {
            name.set("MIT License")
            url.set("https://github.com/elide-dev/v3/blob/v3/LICENSE")
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
