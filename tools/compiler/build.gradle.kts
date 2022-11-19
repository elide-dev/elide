
val plugins = listOf(
  "injekt",
  "interakt",
  "redakt",
  "sekret",
)

tasks.create("buildPlugins") {
  description = "Build all Kotlin compiler plugins"
  dependsOn(plugins.map { ":$it:build" })
}
