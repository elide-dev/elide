plugins {
  kotlin("jvm") version "2.2.20"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
}

dependencies {
  implementation("com.google.guava:guava:33.4.8-jre")
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.20")
}

