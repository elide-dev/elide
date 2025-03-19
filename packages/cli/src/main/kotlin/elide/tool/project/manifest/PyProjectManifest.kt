package elide.tool.project.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JvmRecord @Serializable data class PyProjectManifest(
  @SerialName("build-system") val buildSystem: BuildSystemConfig,
  @SerialName("project") val project: ProjectConfig,
) : PackageManifest {
  @JvmRecord @Serializable data class BuildSystemConfig(
    @SerialName("requires") val requires: List<String> = emptyList(),
    @SerialName("build-backend") val buildBackend: String? = null,
  )

  @JvmRecord @Serializable data class ProjectConfig(
    @SerialName("name") val name: String,
    @SerialName("version") val version: String? = null,
    @SerialName("dependencies") val dependencies: List<String> = emptyList(),
    @SerialName("optional-dependencies") val optionalDependencies: Map<String, List<String>> = emptyMap(),
    @SerialName("requires-python") val requiresPython: String? = null,
    @SerialName("authors") val authors: List<ProjectPerson> = emptyList(),
    @SerialName("maintainers") val maintainers: List<ProjectPerson> = emptyList(),
    @SerialName("description") val description: String? = null,
    @SerialName("readme") val readme: String? = null,
    @SerialName("license") val license: String? = null,
    @SerialName("license-files") val licenseFiles: List<String> = emptyList(),
    @SerialName("keywords") val keywords: List<String> = emptyList(),
    @SerialName("classifiers") val classifiers: List<String> = emptyList(),
    @SerialName("urls") val urls: Map<String, String> = emptyMap(),
    @SerialName("scripts") val scripts: Map<String, String> = emptyMap(),
    @SerialName("gui-scripts") val guiScripts: Map<String, String> = emptyMap(),
    @SerialName("entry-points") val entrypoints: Map<String, Map<String, String>> = emptyMap(),
  )

  @JvmRecord @Serializable data class ProjectPerson(
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
  )
}
