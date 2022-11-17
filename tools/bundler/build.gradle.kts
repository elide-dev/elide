@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

plugins {
  kotlin("jvm")
  id("org.jetbrains.kotlinx.kover")
}

kover {
  isDisabled.set(true)
}
