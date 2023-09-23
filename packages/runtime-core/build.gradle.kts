/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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
  kotlin("jvm")
  kotlin("plugin.serialization")
  
  id("elide.internal.conventions")
}

elide {
  publishing {
    id = "runtime-core"
    name = "Elide Runtime Core"
    description = "Core implementation of the Elide polyglot runtime."

    publish("maven") {
      from(components["kotlin"])
    }
  }

  kotlin {
    target = KotlinTarget.JVM
    explicitApi = true
  }
}

dependencies {
  // Modules
  api(libs.graalvm.polyglot)
  implementation(projects.packages.graalvm)

  // Kotlin / KotlinX
  implementation(kotlin("stdlib"))
  implementation(libs.kotlinx.serialization.core.jvm)
  implementation(libs.kotlinx.serialization.json.jvm)

  // Netty
  implementation(libs.netty.codec.http)
  implementation(libs.netty.codec.http2)
  
  val arch = when (System.getProperty("os.arch")) {
    "amd64", "x86_64" -> "x86_64"
    "arm64", "aarch64", "aarch_64" -> "aarch_64"
    else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
  }
  implementation(libs.netty.tcnative.boringssl.static)
  implementation(libs.netty.transport.native.kqueue)
  implementation(libs.netty.transport.native.kqueue)
  implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
  implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-$arch") })
  implementation(libs.netty.resolver.dns.native.macos)

  implementation(libs.netty.transport.native.epoll)
  implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-$arch") })
  implementation(variantOf(libs.netty.transport.native.iouring) { classifier("linux-$arch") })
  implementation(variantOf(libs.netty.tcnative.boringssl.static) { classifier("linux-$arch") })

  // Testing
  testImplementation(projects.packages.test)
  testImplementation(libs.junit.jupiter.api)
  testRuntimeOnly(libs.junit.jupiter.engine)
}
