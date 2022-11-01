plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    api(kotlin("gradle-plugin"))
    implementation(kotlin("stdlib-jdk7"))
    implementation(gradleApi())
    implementation("com.github.node-gradle:gradle-node-plugin:3.4.0")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:3.1.0") {
        exclude("org.jetbrains.kotlin", "kotlin-sam-with-receiver")
    }
    implementation("org.jetbrains.kotlin:kotlin-sam-with-receiver:${libs.versions.kotlin.get()}")

    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.soy)
    implementation(libs.slf4j)
    implementation(libs.brotli)
    implementation(libs.brotli.native.osx)
    implementation(libs.brotli.native.linux)
    implementation(libs.brotli.native.windows)
    implementation(libs.picocli)
    implementation(libs.picocli.codegen)

    // Protocol Buffers
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.util)
    implementation(libs.protobuf.kotlin)

    implementation(libs.gson)
    implementation(libs.checker)
    implementation(libs.commons.compress)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.truth.proto)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

kotlin {
    // Nothing at this time.
}

sourceSets.getByName("main").java {
    srcDir("src/model/java")
    srcDir("src/model/kotlin")
}

detekt {
    source = files(
        "src/main/java",
    )
}

ktlint {
    filter {
        exclude("**/model/**")
    }
}

gradlePlugin {
    plugins {
        create(PluginCoordinates.ID) {
            id = PluginCoordinates.ID
            implementationClass = PluginCoordinates.IMPLEMENTATION_CLASS
            version = PluginCoordinates.VERSION
        }
    }
}

// Configuration Block for the Plugin Marker artifact on Plugin Central
pluginBundle {
    website = PluginBundle.WEBSITE
    vcsUrl = PluginBundle.VCS
    description = PluginBundle.DESCRIPTION
    tags = PluginBundle.TAGS

    plugins {
        getByName(PluginCoordinates.ID) {
            displayName = PluginBundle.DISPLAY_NAME
        }
    }

    mavenCoordinates {
        groupId = PluginCoordinates.GROUP
        artifactId = PluginCoordinates.ID.removePrefix("$groupId.")
        version = PluginCoordinates.VERSION
    }
}

tasks.create<Copy>("copyNodeRuntimeAssets") {
    description = "Copy runtime Node assets to build root"
    group = "build"
    from("${project.projectDir}/src/main/node/runtime") {
        include("**/*.js")
        include("**/*.json")
    }
    into("${project.buildDir}/elideJsRuntime/sources")
}

tasks.create<Tar>("packageRuntimeAssets") {
    description = "Packages Node runtime code as an embedded tarball"
    group = "build"
    compression = Compression.GZIP

    archiveBaseName.set("js-runtime")
    archiveExtension.set("tar.gz")
    archiveVersion.set("")
    destinationDirectory.set(
        file("${project.buildDir}/resources/main/dev/elide/buildtools/js/runtime")
    )

    dependsOn(
        tasks.named("copyNodeRuntimeAssets")
    )
    into("/") {
        from("${project.buildDir}/elideJsRuntime/sources")
        include(
            "**/*.json",
            "**/*.js",
        )
    }
}

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}

@Suppress("UnstableApiUsage")
tasks.named<ProcessResources>("processResources") {
    dependsOn(
        tasks.named("packageRuntimeAssets")
    )
}
