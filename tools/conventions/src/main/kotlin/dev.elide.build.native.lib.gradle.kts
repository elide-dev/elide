
plugins {
  publishing
  signing
  `java-library`
  kotlin("plugin.serialization")
  id("org.graalvm.buildtools.native")
  id("dev.elide.build.jvm.kapt")
}

tasks.named<Jar>("jar") {
  from("collectReachabilityMetadata")
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["kotlin"])
    }
  }
}
