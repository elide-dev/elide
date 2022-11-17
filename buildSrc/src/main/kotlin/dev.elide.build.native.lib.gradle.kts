
plugins {
  `java-library`
  kotlin("plugin.serialization")
  id("org.graalvm.buildtools.native")
  id("dev.elide.build.jvm.kapt")
}


graalvmNative {
  testSupport.set(true)

  metadataRepository {
    enabled.set(true)
    version.set(Versions.graalvmMetadata)
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
      sharedLibrary.set(true)

      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            }))
          }
        }
      })
    }

    named("test") {
      buildArgs.addAll(listOf(
        "--language:js",
        "--language:regex",
        "--enable-all-security-services",
        "-Dpolyglot.image-build-time.PreinitializeContexts=js",
      ))

      javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of((project.properties["versions.java.language"] as String)))
        if (project.hasProperty("elide.graalvm.variant")) {
          val variant = project.property("elide.graalvm.variant") as String
          if (variant != "COMMUNITY") {
            vendor.set(JvmVendorSpec.matching(when (variant.trim()) {
              "ENTERPRISE" -> "Oracle"
              else -> "GraalVM Community"
            }))
          }
        }
      })
    }
  }
}

tasks.named<Jar>("jar") {
  from("collectReachabilityMetadata")
}
