package elide.tool.project.manifest

import kotlinx.serialization.Serializable

@Serializable @JvmInline value class PythonRequirementsManifest(
  val dependencies: List<String>,
) : PackageManifest
