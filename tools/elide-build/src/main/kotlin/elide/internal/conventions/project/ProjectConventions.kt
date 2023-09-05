package elide.internal.conventions.project

import org.gradle.api.Project
import elide.internal.conventions.Constants.Elide

/** Configure general project properties such as group and version. */
internal fun Project.configureProject() {
  group = Elide.GROUP
  version = Elide.VERSION
}