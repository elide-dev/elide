package elide.internal.conventions.analysis

import org.sonarqube.gradle.SonarExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import elide.internal.conventions.ElideBuildExtension

// Disables code analysis features for for this project
public fun ElideBuildExtension.skipAnalysis() {
  // disable SonarQube
  project.extensions.findByType(SonarExtension::class.java)?.isSkipProject = true

  // disable Kover
  project.extensions.findByType(KoverProjectExtension::class.java)?.disable()
}