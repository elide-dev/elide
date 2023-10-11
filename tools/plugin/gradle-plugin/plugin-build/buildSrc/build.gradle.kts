/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *     https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress(
    "DSL_SCOPE_VIOLATION",
)

val kotlinVersion = "1.9.20-RC"
val javaVersion = "17"

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    api(kotlin("gradle-plugin"))
    api(libs.plugin.kotlin.allopen)
    api(libs.plugin.kotlin.noarg)
    implementation(libs.plugin.kotlinx.serialization)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

afterEvaluate {
    tasks {
        compileKotlin.configure {
            kotlinOptions {
                jvmTarget = javaVersion
                javaParameters = true
            }
        }

        compileTestKotlin.configure {
            kotlinOptions {
                jvmTarget = javaVersion
                javaParameters = true
            }
        }
    }
}
