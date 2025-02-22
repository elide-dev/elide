/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.publishing.publish

plugins {
  alias(libs.plugins.micronaut.graalvm)

  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.allopen")

  alias(libs.plugins.elide.conventions)
}

elide {
  publishing {
    id = "graalvm-js"
    name = "Elide JS integration package for GraalVM"
    description = "Integration package with GraalVM, Elide, and JS."

    publish("jvm") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }

  java {
    // disable module-info processing (not present)
    configureModularity = false
  }

  native {
    target = NativeTarget.LIB
  }

  deps.jarpatch {
    patchClass(libs.graalvm.js.language, "com.oracle.truffle.js.runtime.JSRealm")
    patchResource(libs.graalvm.js.language, "META-INF/services/com.oracle.truffle.api.provider.TruffleLanguageProvider")
  }
}

val embedded: Configuration by configurations.creating {
  // Nothing at this time.
}

dependencies {
  api(projects.packages.engine)
  compileOnly(libs.graalvm.polyglot.js.community)
  compileOnly(libs.graalvm.polyglot.js.isolate)
  compileOnly(libs.graalvm.js.language)
  compileOnly(libs.graalvm.js.isolate)

  // We are replacing this JAR with patched classes, so all classes except excluded ones are copied in.
  embedded(libs.graalvm.js.language)
}

// Where we copy embedded classes before assembly.
val peerClassRoot = layout.buildDirectory.dir("peer-classes")

val unpackPeerClassRoot by tasks.registering(Copy::class) {
  from(zipTree(embedded.filter { it.name.startsWith("js-language") }.singleFile)) {
    exclude("META-INF/services/com.oracle.truffle.api.provider.TruffleLanguageProvider")
  }
  into(peerClassRoot)
}

tasks.processResources.configure {
  dependsOn(unpackPeerClassRoot)
}

tasks.jar.configure {
  dependsOn(unpackPeerClassRoot)
  from(peerClassRoot)
}
