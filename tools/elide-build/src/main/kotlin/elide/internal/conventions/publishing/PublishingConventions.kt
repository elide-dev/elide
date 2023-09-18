package elide.internal.conventions.publishing

import dev.sigstore.sign.SigstoreSignExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import java.net.URI
import elide.internal.conventions.Constants.Build
import elide.internal.conventions.Constants.Credentials
import elide.internal.conventions.Constants.Publishing
import elide.internal.conventions.Constants.Repositories
import elide.internal.conventions.archives.excludeDuplicateArchives

/** Install the publishing plugins and apply common settings for all projects. */
internal fun Project.configurePublishing(
  packageId: String,
  packageName: String,
  packageDescription: String,
) = afterEvaluate {
  // configure publication info (pom.xml)
  extensions.getByType(PublishingExtension::class.java).apply {
    check(publications.isNotEmpty()) { "Cannot setup publishable Elide package '$packageId' with no publications" }
    publications.withType(MavenPublication::class.java) {
      artifactId = artifactId.replace(packageId, "elide-$packageId")
      pom {
        name.set(packageName)
        description.set(packageDescription)
        url.set("https://elide.dev")

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

          developer {
            id.set("darvld")
            name.set("Dario Valdespino")
            email.set("dvaldespino00@gmail.com")
          }
        }

        scm {
          url.set("https://github.com/elide-dev/elide")
        }
      }
    }
  }

  // register publishing task
  tasks.register("publishAllElidePublications") {
    this.group = "Publishing"
    this.description = "Publish all release publications for this Elide package"

    listOf(
      "publishToSonatype",
      "publishAllPublicationsToElideRepository",
      "publishAllPublicationsToGithubRepository",
    ).map(project.tasks::named).map { pubTask ->
      dependsOn(pubTask)
    }
  }

  excludeDuplicateArchives()
}

/**
 * Configures repositories for publishing, namely, the internal GitHub Maven registry for the Elide repo, and a generic
 * repository, configured using project settings.
 *
 * ### Publishing to a custom repository
 *
 * In order to publish to a specific Maven repository, set the `elide.publish.repo.maven` project property (e.g. in
 * `gradle.properties`).
 *
 * If authentication is required, set `elide.publish.repo.maven.auth` to `true`, and either set
 * the `elide.publish.repo.maven.(username|password)` properties, or the `PUBLISH_USER` and `PUBLISH_TOKEN` environment
 * variables.
 */
internal fun Project.configurePublishingRepositories() {
  extensions.getByType(PublishingExtension::class.java).apply {
    // configure repositories for publishing
    repositories.apply {
      // configurable repository, to be overriden by project settings 
      maven {
        name = "elide"
        url = URI.create(project.properties[Publishing.MAVEN_REPO_URL] as String)

        if (project.findProperty(Publishing.MAVEN_AUTH_REQUIRED).toString().toBoolean()) {
          credentials {
            username = (project.properties[Credentials.MAVEN_USER] as? String
              ?: System.getenv(Credentials.PUBLISH_USER))?.ifBlank { null }

            password = (project.properties[Credentials.MAVEN_PASSWORD] as? String
              ?: System.getenv(Credentials.PUBLISH_TOKEN))?.ifBlank { null }
          }
        }
      }

      // GitHub Maven registry
      maven {
        name = "github"
        url = uri(Repositories.GITHUB_MAVEN)

        credentials {
          username = System.getenv(Credentials.GITHUB_ACTOR)
          password = System.getenv(Credentials.GITHUB_TOKEN)
        }
      }
    }
  }
}

/** Register the "javadocJar" task and include it in maven publications. */
internal fun Project.publishJavadocJar() {
  val buildDocs = findProperty(Build.BUILD_DOCS)?.toString()?.toBoolean() ?: true

  extensions.findByType(PublishingExtension::class.java)?.apply {
    publications.withType(MavenPublication::class.java) {
      if (buildDocs) artifact(tasks.named("javadocJar"))
    }
  }
}

/** Include the sources Jar task in publications. */
internal fun Project.publishSourcesJar() {
  extensions.findByType(PublishingExtension::class.java)?.apply {
    publications.withType(MavenPublication::class.java) {
      artifact(tasks.named("sourcesJar"))
    }
  }
}

/** Configure signing for all of this project's publications and archive tasks. */
internal fun Project.configureSigning() {
  // resolve publishing extension (so we can sign publications)
  val publishing = extensions.getByType(PublishingExtension::class.java)

  extensions.getByType(SigningExtension::class.java).apply {
    if (findProperty("enableSigning").toString().toBoolean()) {
      // sign all archives (JAR and ZIP files)
      sign(configurations.getByName("archives"))

      // sign all publications
      sign(publishing.publications)
    }
  }

  // configure publishing tasks to depend on signing
  val signingTasks = tasks.withType(Sign::class.java)
  tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    dependsOn(signingTasks)
  }
}

internal fun Project.configureSigstore() {
  extensions.getByType(SigstoreSignExtension::class.java).apply {
    oidcClient.apply {
      gitHub {
        audience.set("sigstore")
      }
      web {
        clientId.set("sigstore")
        issuer.set("https://oauth2.sigstore.dev/auth")
      }
    }
  }
}