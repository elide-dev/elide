import GradleProject.projectConstants

plugins {
  id("dev.elide.build.kotlin.compilerPlugin")
}


project.projectConstants(
  packageName = "elide.tools.kotlin.plugin.injekt",
  extraProperties = mapOf(
    "PLUGIN_ID" to Constant.string("injekt"),
  )
)
