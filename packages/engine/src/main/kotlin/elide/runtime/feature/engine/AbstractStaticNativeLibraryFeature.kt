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

package elide.runtime.feature.engine

import com.oracle.svm.core.jdk.NativeLibrarySupport
import com.oracle.svm.core.jdk.PlatformNativeLibrarySupport
import com.oracle.svm.hosted.FeatureImpl
import com.oracle.svm.hosted.jni.JNIFeature
import org.graalvm.nativeimage.hosted.Feature
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedList
import kotlin.io.path.Path
import elide.runtime.feature.NativeLibraryFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.DUAL
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.SHARED
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC

public abstract class AbstractStaticNativeLibraryFeature : NativeLibraryFeature {
  private companion object {
    private val preferShared = System.getProperty("elide.preferShared") == "true"
    private val staticUmbrella = System.getProperty("elide.staticUmbrella") == "true"
  }

  @Suppress("LongParameterList")
  protected fun libraryNamed(
    name: String,
    vararg layout: String,
    type: NativeLibraryFeature.NativeLibType = STATIC,
    linkName: String = name,
    builtin: Boolean = true,
    registerJni: Boolean = true,
    registerPrefix: Boolean = builtin,
    eager: Boolean = false,
    absolutePath: Path? = null,
    absoluteLibs: Pair<Path?, Path?>? = null,
    initializer: Boolean = false,
    deps: List<String> = emptyList(),
  ): NativeLibraryFeature.NativeLibInfo = NativeLibraryFeature.NativeLibInfo.Companion.of(
    name,
    *layout,
    type = type,
    prefix = registerPrefix,
    jni = registerJni,
    builtin = builtin,
    eager = eager,
    linkName = linkName,
    absolutePath = absolutePath,
    absoluteLibs = absoluteLibs,
    initializer = initializer,
    deps = deps,
  )

  // Scan each provided JAR file system path (as a ZipFileSystem) for the resource at the provided path; when a match is
  // found, it is returned as an `InputStream`, otherwise an error is thrown if the resource is not found.
  private fun scanJarsForResource(jars: List<Path>, path: String): InputStream {
    for (jar in jars) {
      val env = mapOf("enablePosixFileAttributes" to "true")
      requireNotNull(FileSystems.newFileSystem(jar, env)) { "JAR at path not found: '$jar'" }.use {
        try {
          return it.getPath(path).toUri().toURL().openStream()
        } catch (_: FileNotFoundException) {
          // ignore
        }
      }
    }
    error("Resource not found in any JAR: '$path' in JARs: '${jars.joinToString(", ")}'")
  }

  private fun Feature.BeforeAnalysisAccess.scanForResource(jarToken: String, path: String): InputStream? {
    val candidates = applicationClassPath.filter { it.toString().contains(jarToken) }
    if (candidates.isEmpty()) {
      return null
    }
    return scanJarsForResource(candidates, path)
  }

  protected fun Feature.BeforeAnalysisAccess.unpackLibrary(
    jar: String,
    name: String,
    arch: String,
    vararg path: String,
    renameTo: ((String) -> String)? = null,
    cbk: (() -> Unit)? = null,
  ): List<NativeLibraryFeature.UnpackedNative> {
    val nativesPath = requireNotNull(System.getProperty("elide.natives")?.ifBlank { null })
    val unpacked: LinkedList<NativeLibraryFeature.UnpackedNative> = LinkedList()
    for (candidate in path) {
      val stream = scanForResource(jar, candidate) ?: continue
      stream.use {
        val target = Path(nativesPath)
          .resolve(
            renameTo?.let { Path(it.invoke(candidate)) } ?: Path(candidate).fileName.let {
              val filename = it.fileName
              val dylibName = filename.toString().replace(".jnilib", ".dylib")
              val parent = it.parent
              if (parent != null) {
                it.parent.resolve(dylibName)
              } else {
                it.resolveSibling(dylibName)
              }
            },
          )
          .toFile()

        val exists = target.exists()
        val size = if (exists) Files.size(target.toPath()) else -1

        if (!exists || size != stream.available().toLong()) {
          target.parentFile?.let { if (!it.exists()) it.mkdirs() }
          target.outputStream().use { it.write(stream.readBytes()) }
        }
        cbk?.invoke()
        unpacked.add(NativeLibraryFeature.UnpackedNative(name, candidate, arch, target.toPath().toAbsolutePath()))
      }
    }
    return unpacked
  }

  protected fun nativeLibrary(
    singular: NativeLibraryFeature.NativeLibInfo? = null,
  ): NativeLibraryFeature.NativeLibInfo? {
    return singular
  }

  protected fun nativeLibrary(
    darwin: NativeLibraryFeature.NativeLibInfo? = null,
    linux: NativeLibraryFeature.NativeLibInfo? = null,
    windows: NativeLibraryFeature.NativeLibInfo? = null,
  ): NativeLibraryFeature.NativeLibInfo? {
    return when (val os = System.getProperty("os.name", "unknown").lowercase().trim()) {
      "mac os x" -> darwin
      "linux" -> linux
      "windows" -> windows
      else -> error("unknown os: $os")
    }
  }

  public abstract fun nativeLibs(access: Feature.BeforeAnalysisAccess): List<NativeLibraryFeature.NativeLibInfo?>

  public open fun unpackNatives(access: Feature.BeforeAnalysisAccess): List<NativeLibraryFeature.UnpackedNative> {
    // native libraries to unpack at build time
    return emptyList()
  }

  override fun getRequiredFeatures(): MutableList<Class<out Feature>> {
    return mutableListOf(
      JNIFeature::class.java,
    )
  }

  @Suppress("SpreadOperator", "ComplexCondition")
  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
    unpackNatives(access)
    super.beforeAnalysis(access)

    nativeLibs(access).forEach {
      if (it == null) return@forEach // not supported on this platform

      // we should only load this library if we are not in static umbrella mode; in that mode, only the native umbrella
      // library is loaded, and all other libraries are statically linked into that.
      val shouldLoad = !staticUmbrella

      // register lib
      val libSupport = NativeLibrarySupport.singleton()
      val platformLibs = PlatformNativeLibrarySupport.singleton()

      if (it.builtin) {
        libSupport.preregisterUninitializedBuiltinLibrary(it.linkName)
      }
      if (it.registerJni) {
        (access as FeatureImpl.BeforeAnalysisAccessImpl).nativeLibraries.let { nativeLibraries ->
          if (it.registerPrefix) {
            it.prefix.forEach { prefix ->
              platformLibs.addBuiltinPkgNativePrefix(prefix.replace(".", "_"))
            }
          }
          if (shouldLoad) {
            when (it.type) {
              STATIC -> nativeLibraries.addStaticJniLibrary(it.name, *it.deps.toTypedArray())
              SHARED -> nativeLibraries.addDynamicNonJniLibrary(it.name)
              DUAL -> if (preferShared) {
                nativeLibraries.addDynamicNonJniLibrary(it.name)
              } else {
                nativeLibraries.addStaticJniLibrary(it.name, *it.deps.toTypedArray())
              }
            }
          }
        }
      } else if (shouldLoad) {
        (access as FeatureImpl.BeforeAnalysisAccessImpl).nativeLibraries.let { nativeLibraries ->
          when (it.type) {
            STATIC -> nativeLibraries.addStaticNonJniLibrary(it.name, *it.deps.toTypedArray())
            SHARED -> nativeLibraries.addDynamicNonJniLibrary(it.name)
            DUAL -> if (preferShared) {
              nativeLibraries.addDynamicNonJniLibrary(it.name)
            } else {
              nativeLibraries.addStaticJniLibrary(it.name, *it.deps.toTypedArray())
            }
          }
        }
      }
      if (shouldLoad && it.eager) {
        val absolute = it.absolutePath
        if (absolute != null) {
          libSupport.loadLibraryAbsolute(absolute.toFile())
        }
      }
      if (!it.builtin && !it.registerPrefix && !it.registerJni && !it.eager) {
        error("Nothing to do for library '$it'")
      }
    }
  }

  public fun method(clazz: Class<*>, methodName: String, vararg args: Class<*>): Method {
    return clazz.getDeclaredMethod(methodName, *args)
  }

  public fun fields(clazz: Class<*>, vararg fieldNames: String): Array<Field?> {
    val fields = arrayOfNulls<Field>(fieldNames.size)

    for (i in fieldNames.indices) {
      fields[i] = clazz.getDeclaredField(fieldNames[i])
    }
    return fields
  }
}
