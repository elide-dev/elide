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
@file:Suppress("SpreadOperator", "unused")

package elide.runtime.feature.engine

import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl
import org.graalvm.nativeimage.Platform
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import kotlin.io.path.Path
import elide.annotations.engine.VMFeature
import elide.runtime.feature.NativeLibraryFeature.UnpackedNative

/** Registers native crypto libraries for static JNI. */
@VMFeature internal class NativeCryptoFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private const val PROVIDED = "provided"
    private const val BORINGSSL_STATIC_JAR: String = "netty-tcnative-boringssl-static"
    private const val ARCH_PROP: String = "os.arch"
    private const val OS_PROP: String = "os.name"
    private val staticJni = java.lang.Boolean.getBoolean("elide.staticJni")

    private val tcnative = arrayOf(
      "io.netty.handler.ssl.OpenSsl",
      "io.netty.internal.tcnative.Buffer",
      "io.netty.internal.tcnative.Library",
      "io.netty.internal.tcnative.NativeStaticallyReferencedJniMethods",
      "io.netty.internal.tcnative.SSL",
      "io.netty.internal.tcnative.SSLContext",
      "io.netty.internal.tcnative.SSLSession",
    )

    private val libtransport_a = Path(System.getProperty("elide.natives")).resolve("libtransport.a")
    private val libtransport_dylib = Path(System.getProperty("elide.natives")).resolve("libtransport.dylib")
    private val libtransport_so = Path(System.getProperty("elide.natives")).resolve("libtransport.so")
  }

  override fun getDescription(): String = "Registers native crypto libraries"

  private fun initializeTcNative() {
    io.netty.handler.ssl.OpenSsl.ensureAvailability()
    io.netty.internal.tcnative.Library.initialize(PROVIDED, null)
  }

  // static mode only; otherwise, standard JNI is used
  override fun isInConfiguration(access: IsInConfigurationAccess?): Boolean = staticJni

  private fun renameNativeCryptoLib(lib: String): String {
    val filename = lib.split("/").last().substringBeforeLast('.')
    return if (filename != "libtcnative") Path(lib).last().toString() else {
      val arch = System.getProperty(ARCH_PROP)
        .replace("-", "_")
        .replace("aarch64", "aarch_64")
        .replace("amd64", "x86_64")
      val os = System.getProperty(OS_PROP).replace("Mac OS X", "osx").replace("Linux", "linux")
      val extension = lib.substringAfterLast('.')
      "libnetty_tcnative_${os}_${arch}.$extension"
    }
  }

  override fun unpackNatives(access: BeforeAnalysisAccess): List<UnpackedNative> = if (!staticJni) when {
    /* Dynamic JNI */

    Platform.includedIn(Platform.LINUX::class.java) -> when (System.getProperty(ARCH_PROP)) {
      "x86_64", "amd64" -> access.unpackLibrary(
        BORINGSSL_STATIC_JAR,
        "netty_tcnative_linux_x86_64",
        "x86-64",
        "META-INF/native/libnetty_tcnative_linux_x86_64.so",
      ) { initializeTcNative() }

      "aarch64", "arm64" -> access.unpackLibrary(
        BORINGSSL_STATIC_JAR,
        "netty_tcnative_linux_aarch_64",
        "aarch64",
        "META-INF/native/libnetty_tcnative_linux_aarch_64.so",
      ) { initializeTcNative() }

      // no support on this architecture
      else -> emptyList()
    }

    Platform.includedIn(Platform.DARWIN::class.java) -> when (System.getProperty(ARCH_PROP)) {
      "x86_64", "amd64" -> access.unpackLibrary(
        BORINGSSL_STATIC_JAR,
        "netty_tcnative_osx_x86_64",
        "x86-64",
        "META-INF/native/libnetty_tcnative_osx_x86_64.jnilib",
        renameTo = { "libnetty_tcnative_osx_x86_64.dylib" },
      ) { initializeTcNative() }

      "aarch64", "arm64" -> access.unpackLibrary(
        BORINGSSL_STATIC_JAR,
        "netty_tcnative_osx_aarch_64",
        "aarch64",
        "META-INF/native/libnetty_tcnative_osx_aarch_64.jnilib",
        renameTo = { "libnetty_tcnative_osx_aarch_64.dylib" },
      ) { initializeTcNative() }

      // no support on this architecture
      else -> emptyList()
    }

    // no support on this platform
    else -> emptyList()
  } else emptyList()  // static JNI unpacks no libraries now

  override fun nativeLibs(access: BeforeAnalysisAccess) = when (val arch = System.getProperty(ARCH_PROP)) {
    "x86_64", "amd64" -> "x86_64"
    "aarch64", "arm64" -> "aarch_64"
    else -> error("Unsupported architecture: $arch")
  }.let { archTag ->
    when (staticJni) {
      false -> listOf(
        nativeLibrary(
          darwin = libraryNamed(
            "netty_tcnative_osx_$archTag",
            *tcnative,
            builtin = true,
          ),
          linux = libraryNamed(
            "netty_tcnative_linux_$archTag",
            *tcnative,
            builtin = true,
          ),
        )
      )

      true -> listOf(
        nativeLibrary(
          darwin = libraryNamed(
            "transport",
            *tcnative,
            builtin = true,
            initializer = true,
            absolutePath = libtransport_a,
            absoluteLibs = (libtransport_a to libtransport_dylib),
          ),
          linux = libraryNamed(
            "transport",
            *tcnative,
            builtin = true,
            initializer = true,
            absolutePath = libtransport_a,
            absoluteLibs = (libtransport_a to libtransport_so),
          ),
        )
      )
    }
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)

    listOf(
      "apr-2" to arrayOf("ssl", "crypto"),
      "crypto" to arrayOf(),
      "ssl" to arrayOf("crypto"),
      "sqlite3" to arrayOf(),
    ).forEach { (lib, deps) ->
      // even though these are not JNI libraries, we need to add them as JNI libraries, so that they are emitted as
      // static archives to be included in the final binary.
      //
      // this is probably a bug in graalvm.
      (access as BeforeAnalysisAccessImpl).nativeLibraries.addStaticJniLibrary(lib, *deps)
    }
  }
}
