/*
 * Copyright (c) 2023-2024 Elide Ventures, LLC.
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

import elide.internal.conventions.elide
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.publishing.publish

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("elide.internal.conventions")
}

kotlin {
  sourceSets {
    val commonMain by getting
    val commonTest by getting
    val defaultMain by creating { dependsOn(commonMain) }
    val defaultTest by creating { dependsOn(commonTest) }
  }
}

elide {
  publishing {
    id = "http"
    name = "Elide HTTP"
    description = "Cross-platform HTTP utilities and wrappers."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.Embedded
    explicitApi = true
    splitJvmTargets = true
  }

  java {
    configureModularity = false
  }
}

group = "dev.elide"
version = rootProject.version as String

dependencies {
//  common {
//    implementation(projects.packages.base)
//    implementation(projects.packages.core)
//  }
//
//  commonTest {
//    // Testing
//    implementation(projects.packages.test)
//    implementation(kotlin("test"))
//  }
//
//  jvm {
//    implementation(mn.micronaut.http)
//  }
}
