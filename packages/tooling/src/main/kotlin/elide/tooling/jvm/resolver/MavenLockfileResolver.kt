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
package elide.tooling.jvm.resolver

import java.nio.file.Path
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListMap
import elide.tooling.Classpath
import elide.tooling.ClasspathProvider
import elide.tooling.ClasspathSpec
import elide.tooling.ClasspathsProvider
import elide.tooling.MultiPathUsage
import elide.tooling.lockfile.ElideLockfile.MavenArtifact
import elide.tooling.lockfile.ElideLockfile.MavenLockfile
import elide.tooling.lockfile.ElideLockfile.MavenUsageType

/**
 * ## Maven Lockfile Resolver
 *
 * Capable of consuming an Elide lockfile stanza injected by the Maven resolver, and then rebuilding classpaths from
 * that static data, with minimal runtime resolution; this resolver should be used in circumstances where dependencies
 * are needed, but a full resolution cycle is not desired.
 *
 * @property lockfile Maven's portion of the Elide lockfile.
 */
public class MavenLockfileResolver internal constructor (
  lockfile: MavenLockfile,
  private val rootPath: Path,
) : ClasspathsProvider {
  public companion object {
    @JvmStatic private val m2DepsPrefix by lazy {
      Path.of(".dev").resolve("dependencies")
    }

    @JvmStatic public fun of(lockfile: MavenLockfile, rootPath: Path): MavenLockfileResolver {
      return MavenLockfileResolver(lockfile, rootPath)
    }
  }

  // Registry of library coordinates to their artifacts.
  private val registry = ConcurrentSkipListMap<String, MavenArtifact>()

  // Registry of usages to their artifacts; held as a list to allow order preservation.
  private val registryByUsage = ConcurrentSkipListMap<MultiPathUsage, MutableList<MavenArtifact>>()

  // Registry of packages to their usages.
  private val registryByPackage = ConcurrentSkipListMap<MavenArtifact, MutableSet<MultiPathUsage>>()

  init {
    // we will use this map to decode the lockfile payload; after this step, it can be discarded.
    val localIdMap = TreeMap<UInt, MavenArtifact>()

    // build the classpath itself first, which holds all artifacts.
    lockfile.classpath.forEach { entry ->
      registry[entry.coordinate] = entry
      localIdMap[entry.id] = entry
    }

    // based on expressed usage, build a map of usages to their artifacts, and a map of artifacts to their usages.
    lockfile.usage.flatMap { usageInfo ->
      val entry = requireNotNull(localIdMap[usageInfo.id]) {
        "Invalid reference: '${usageInfo.id}' in classpath entry map"
      }
      usageInfo.types.map { usage ->
        usage to entry
      }
    }.map { (usage, entry) ->
      val mpUsage = when (usage) {
        MavenUsageType.COMPILE -> MultiPathUsage.Compile
        MavenUsageType.TEST -> MultiPathUsage.TestCompile
        MavenUsageType.PROCESSORS -> MultiPathUsage.Processors
        MavenUsageType.TEST_PROCESSORS -> MultiPathUsage.TestProcessors
        MavenUsageType.RUNTIME -> MultiPathUsage.Runtime
        MavenUsageType.TEST_RUNTIME -> MultiPathUsage.TestRuntime
        MavenUsageType.MODULES -> MultiPathUsage.Modules
        MavenUsageType.DEV_ONLY -> MultiPathUsage.Dev
      }
      registryByUsage.getOrPut(mpUsage) { mutableListOf() }.also {
        it.add(entry)
      }
      registryByPackage.getOrPut(entry) { TreeSet() }.also {
        it.add(mpUsage)
      }
    }
  }

  // Match any held packages by the provided `spec`; if no spec is provided, return all artifacts.
  private fun matchPackagesBySpec(spec: ClasspathSpec?): Sequence<MavenArtifact> = sequence {
    when (spec) {
      null -> yieldAll(registry.values)
      else -> registry.filter { entry ->
        when (val candidate = spec.usage) {
          null -> error(
            "No support for named classpath filtering from lockfile context"
          )
          else -> when (val usages = registryByPackage[entry.value]) {
            null -> false
            else -> candidate.expand().any {
              it in usages
            }
          }
        }
      }.forEach {
        yield(it.value)
      }
    }
  }

  // Converts a relative path to an artifact to an absolute path.
  private fun resolveArtifact(relative: String): Path {
    return rootPath.resolve(m2DepsPrefix).resolve(relative)
  }

  override suspend fun classpathProvider(spec: ClasspathSpec?): ClasspathProvider? = when (spec) {
    null -> ClasspathProvider {
      // return the entire suite of all dependencies. we do this by matching against a null-spec, and then re-forming
      // the classpath container from the inflated artifact paths on-disk.
      Classpath.from(matchPackagesBySpec(null).map { resolveArtifact(it.artifact) }.toList())
    }
    else -> ClasspathProvider {
      // filter by usage type
      Classpath.from(matchPackagesBySpec(spec).map { resolveArtifact(it.artifact) }.toList())
    }
  }
}
