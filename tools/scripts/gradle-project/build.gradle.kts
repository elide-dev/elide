plugins {
  java
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType(JavaCompile::class).configureEach {
  options.isFork = true
  options.compilerArgs.addAll(
    listOf(
      "--release", "8",
    )
  )
  options.forkOptions.executable = System.getenv("JAVA_HOME") + "/bin/elide-javac"
}
