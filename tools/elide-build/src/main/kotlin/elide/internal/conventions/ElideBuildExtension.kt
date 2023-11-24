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

package elide.internal.conventions

import org.gradle.api.Project
import elide.internal.conventions.kotlin.KotlinTarget
import elide.internal.conventions.native.NativeTarget
import elide.internal.conventions.native.NativeTarget.APP

public class ElideBuildExtension internal constructor(internal val project: Project) {
  public sealed class Convention(internal val project: Project) {
    internal open var requested: Boolean = false
  }

  /** Configuration for tasks that create JAR, ZIP, and TAR archives. */
  public class Archives(project: Project) : Convention(project) {
    // on by default
    override var requested: Boolean = true

    /** Adjust task settings to improve build caching/hermeticity. */
    public var reproducibleTasks: Boolean = true

    /** Whether to exclude (ignore) the archive if it has already been included. */
    public var excludeDuplicates: Boolean = false
  }

  /** Configuration for publishable targets. */
  public class Publishing(project: Project) : Convention(project) {
    public var enableSigstore: Boolean = false

    /** Whether to sign all publications. */
    public var signPublications: Boolean = true

    /** Whether to configure common repositories like the Elide GitHub Maven registry. */
    public var commonRepositories: Boolean = true

    /** The ID of the published artifact, defaults to the name of the project. */
    public var id: String = project.name

    /** The name of the published artifact. */
    public var name: String = project.displayName

    /** A description for the published artifact. */
    public var description: String = project.description ?: ""
  }

  /** Configuration for Kotlin compilations. */
  public class Kotlin(project: Project) : Convention(project) {
    /** Target platform for Kotlin compilation. */
    public var target: KotlinTarget? = null

    /** Whether this project uses KAPT. Only allowed for [jvm] projects. */
    public var kapt: Boolean = false

    /** Whether this project uses KSP. Only allowed for [jvm] projects. */
    public var ksp: Boolean = false

    /** Whether to configure the Kotlin AllOpen plugin with predefined settings. */
    public var allOpen: Boolean = false

    /** Whether to configure the Kotlin NoArgs plugin with predefined settings. */
    public var noArgs: Boolean = false

    /** Whether to enable explicit API mode. */
    public var explicitApi: Boolean = false

    /** Custom Kotlin compiler args to apply. */
    public var customKotlinCompilerArgs: MutableList<String> = ArrayList()
  }

  /** Configuration for Java compilations */
  public class Java(project: Project) : Convention(project) {
    /** Whether to include the "sources" JAR. */
    public var includeSources: Boolean = true

    /** Whether to include the "javadoc" JAR. */
    public var includeJavadoc: Boolean = true

    /** Whether to configure Java 9 modularity. */
    public var configureModularity: Boolean = true
  }

  /** Configuration for the JVM platform */
  public class Jvm(project: Project) : Convention(project) {
    /** Whether to align all JVM versions across Kotlin and Java tasks. */
    public var alignVersions: Boolean = true

    /** Whether to force all JVM-targeting tasks to use JVM 17. Defaults to `false`. */
    public var forceJvm17: Boolean = false
  }

  /** Configuration for testing tasks */
  public class Testing(project: Project) : Convention(project) {
    // on by default
    override var requested: Boolean = true

    /** Whether to configure code coverage reports with Kover */
    public var kover: Boolean = false

    /** Whether to configure code coverage reports with Jacoco */
    public var jacoco: Boolean = false
  }

  /** Configuration for Docker-related tasks */
  public class Docker(project: Project) : Convention(project) {
    /** Whether to read from the local GCloud credentials file and use them to authenticate with Docker registries. */
    public var useGoogleCredentials: Boolean = false
  }

  /** Configuration for GraalVM Native builds. */
  public class Native(project: Project) : Convention(project) {
    /** Whether to publish native targets, usually for native libaries. */
    public var publish: Boolean = false

    /** The kind of compilation for this project. This affects the options passed to the Native Image compiler. */
    public var target: NativeTarget = APP

    /** Whether to enable the use of GraalVM's agent for native compilation. */
    public var useAgent: Boolean = false

    /** Whether to resolve a specific GraalVM distribution and use it as launcher for [APP] targets. */
    public var configureLauncher: Boolean = false
  }

  internal val archives = Archives(project)
  internal val publishing = Publishing(project)
  internal val kotlin = Kotlin(project)
  internal val java = Java(project)
  internal val jvm = Jvm(project)
  internal val testing = Testing(project)
  internal val docker = Docker(project)
  internal val native = Native(project)

  /** Whether to use dependency locking. */
  public var lockDependencies: Boolean = true

  /** Whether to fail on dependency version conflicts. */
  public var strictDependencies: Boolean = true

  private fun <T : Convention> configure(target: T, block: T.() -> Unit) {
    target.requested = true
    target.apply(block)
  }

  /** Configure tasks that create JAR, ZIP, and TAR archives. */
  public fun archives(block: Archives.() -> Unit = { }): Unit = configure(archives, block)

  /** Configure publishable targets. */
  public fun publishing(block: Publishing.() -> Unit = { }): Unit = configure(publishing, block)

  /** Configure Kotlin compilations. */
  public fun kotlin(block: Kotlin.() -> Unit = { }): Unit = configure(kotlin, block)

  /** Configure Java compilations. */
  public fun java(block: Java.() -> Unit = { }): Unit = configure(java, block)

  /** Configure JVM platform settings. */
  public fun jvm(block: Jvm.() -> Unit = { }): Unit = configure(jvm, block)

  /** Configure test tasks. */
  public fun testing(block: Testing.() -> Unit = { }): Unit = configure(testing, block)

  /** Configure Docker settings. */
  public fun docker(block: Docker.() -> Unit = { }): Unit = configure(docker, block)

  /** Configure GraalVM native compilations. */
  public fun native(block: Native.() -> Unit = { }): Unit = configure(native, block)
}
