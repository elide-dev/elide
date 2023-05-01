dependencyResolutionManagement {
  repositories {
    maven("https://maven.pkg.st/")
    maven("https://gradle.pkg.st/")
    maven("https://elide.pkg.st/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/elide.versions.toml"))
    }
  }
}

pluginManagement {
  repositories {
    maven("https://gradle.pkg.st/")
    maven("https://elide.pkg.st/")
    maven("https://maven.pkg.st/")
  }
}

//plugins {
//  id("dev.elide.build.jvm.toolchains")
//}

rootProject.name = "elideBuild"
