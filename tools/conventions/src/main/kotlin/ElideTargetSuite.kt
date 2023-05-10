import Java9Modularity.prepMultiReleaseJar
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/** Configure a multi-release JAR target suite. */
public object ElideTargetSuite {
  /** Target versions to include in the multi-release JAR. */
  private val targetVersions = sortedSetOf(
    JavaVersion.VERSION_17,
    JavaVersion.VERSION_19,
  )

  /** Entrypoint for libraries which need multi-release functionality. */
  @JvmStatic public fun Project.configureMultiReleaseJar() = prepMultiReleaseJar(
    multiRelease = true,
    fullBuild = true,
    targetVersions = JavaVersion.values().filter { targetVersions.contains(it) }.toTypedArray(),
  )
}
