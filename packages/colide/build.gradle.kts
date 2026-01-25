/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
import elide.internal.conventions.publishing.publish

plugins {
    kotlin("jvm")
    alias(libs.plugins.elide.conventions)
}

elide {
    publishing {
        id = "colide"
        name = "Colide OS Native Drivers"
        description = "JNI bindings for Colide OS bare metal drivers (VESA, keyboard, AI)"

        publish("jvm") {
            from(components["kotlin"])
        }
    }

    kotlin {
        target = KotlinTarget.JVM
        explicitApi = true
    }

    java {
        configureModularity = false
    }

    checks {
        diktat = false
    }
}

dependencies {
    api(projects.packages.engine)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
