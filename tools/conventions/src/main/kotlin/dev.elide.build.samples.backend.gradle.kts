
plugins {
  id("io.micronaut.application")
  id("io.micronaut.aot")
  id("io.micronaut.graalvm")
  id("io.micronaut.docker")
  id("dev.elide.build.jvm.kapt")
  id("dev.elide.build.docker")
}

val quickbuild = (
  project.properties["elide.release"] != "true" ||
  project.properties["elide.buildMode"] == "dev"
)

graalvmNative {
  testSupport.set(true)

  metadataRepository {
    enabled.set(true)
    version.set(GraalVMVersions.graalvmMetadata)
  }

  agent {
    defaultMode.set("standard")
    builtinCallerFilter.set(true)
    builtinHeuristicFilter.set(true)
    enableExperimentalPredefinedClasses.set(false)
    enableExperimentalUnsafeAllocationTracing.set(false)
    trackReflectionMetadata.set(true)
    enabled.set(true)

    modes {
      standard {}
    }
    metadataCopy {
      inputTaskNames.add("test")
      outputDirectories.add("src/main/resources/META-INF/native-image")
      mergeWithExisting.set(true)
    }
  }

  binaries {
    named("main") {
      fallback.set(false)
      sharedLibrary.set(false)
      quickBuild.set(quickbuild)
    }

    named("test") {
      quickBuild.set(quickbuild)
    }
  }
}

tasks.withType<Tar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip>{
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/base:latest")
}

tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("optimizedDockerfile") {
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/jvm17")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native:latest")
  args("-H:+StaticExecutableWithDynamicLibC")
}

tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("optimizedDockerfileNative") {
  graalImage.set("${project.properties["elide.publish.repo.docker.tools"]}/builder:latest")
  baseImage("${project.properties["elide.publish.repo.docker.tools"]}/runtime/native:latest")
  args("-H:+StaticExecutableWithDynamicLibC")
}

tasks.named<JavaExec>("run") {
  val argsList = ArrayList<String>()
  jvmArgs(listOf(
    "-Delide.dev=true",
  ))
  if (project.hasProperty("elide.vm.inspect") && project.properties["elide.vm.inspect"] == "true") {
    argsList.add("--elide.vm.inspect=true")
  } else {
    argsList.add("--elide.vm.inspect=false")
  }
  @Suppress("SpreadOperator")
  args(
    *argsList.toTypedArray()
  )
}
