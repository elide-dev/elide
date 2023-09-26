package elide.internal.conventions.publishing

import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import elide.internal.conventions.ElideBuildExtension

public fun ElideBuildExtension.Publishing.publish(
  name: String,
  block: MavenPublication.() -> Unit = { }
): Publication = project.extensions
  .getByType(PublishingExtension::class.java)
  .publications
  .create(name, MavenPublication::class.java, block)
