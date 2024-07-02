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
package elide.runtime.feature.engine

import com.oracle.svm.hosted.FeatureImpl.BeforeAnalysisAccessImpl
import org.graalvm.nativeimage.Platform
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import kotlin.io.path.Path
import elide.annotations.internal.VMFeature
import elide.runtime.feature.NativeLibraryFeature.UnpackedNative

/** Registers native crypto libraries for static JNI. */
@VMFeature internal class NativeCryptoFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private const val STATIC_JNI = true
    private const val PROVIDED = "provided"
    private const val BORINGSSL_STATIC_JAR: String = "netty-tcnative-boringssl-static"
    private const val TCNATIVE_JAR: String = "tcnative"
    private const val ARCH_PROP: String = "os.arch"
    private const val OS_PROP: String = "os.name"

    private val tcnative = arrayOf(
      "io.netty.handler.ssl.OpenSsl",
      "io.netty.internal.tcnative.Buffer",
      "io.netty.internal.tcnative.Library",
      "io.netty.internal.tcnative.NativeStaticallyReferencedJniMethods",
      "io.netty.internal.tcnative.SSL",
      "io.netty.internal.tcnative.SSLContext",
      "io.netty.internal.tcnative.SSLSession",
    )
  }

  override fun getDescription(): String = "Registers native crypto libraries"

  private fun initializeTcNative() {
    io.netty.handler.ssl.OpenSsl.ensureAvailability()
    io.netty.internal.tcnative.Library.initialize(PROVIDED, null)
  }

  override fun isInConfiguration(access: IsInConfigurationAccess?): Boolean {
    return STATIC_JNI  // static mode only; otherwise, standard JNI is used
  }

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

  override fun unpackNatives(access: BeforeAnalysisAccess): List<UnpackedNative> = if (!STATIC_JNI) when {
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
  } else when {
    /* Static JNI */

    Platform.includedIn(Platform.LINUX_AMD64::class.java) -> access.unpackLibrary(
      TCNATIVE_JAR,
      "netty_tcnative_linux_x86_64",
      "x86-64",
      "META-INF/native/linux/amd64/libapr-2.so",
      "META-INF/native/linux/amd64/libapr-2.a",
      "META-INF/native/linux/amd64/libcrypto.a",
      "META-INF/native/linux/amd64/libssl.a",
      "META-INF/native/linux/amd64/shared/libtcnative.so",
      "META-INF/native/linux/amd64/static/libtcnative.a",
      renameTo = ::renameNativeCryptoLib,
    )

    Platform.includedIn(Platform.LINUX_AARCH64::class.java) -> access.unpackLibrary(
      TCNATIVE_JAR,
      "netty_tcnative_linux_aarch64",
      "aarch64",
      "META-INF/native/linux/aarch64/libapr-2.so",
      "META-INF/native/linux/aarch64/libapr-2.a",
      "META-INF/native/linux/aarch64/libcrypto.a",
      "META-INF/native/linux/aarch64/libdecrepit.a",
      "META-INF/native/linux/aarch64/libssl.a",
      "META-INF/native/linux/aarch64/shared/libtcnative.so",
      "META-INF/native/linux/aarch64/static/libtcnative.a",
      renameTo = ::renameNativeCryptoLib,
    )

    Platform.includedIn(Platform.DARWIN_AMD64::class.java) -> access.unpackLibrary(
      TCNATIVE_JAR,
      "netty_tcnative_osx_x86_64",
      "x86-64",
      "META-INF/native/macos/amd64/libapr-2.dylib",
      "META-INF/native/macos/amd64/libapr-2.a",
      "META-INF/native/macos/amd64/libcrypto.a",
      "META-INF/native/macos/amd64/libdecrepit.a",
      "META-INF/native/macos/amd64/libssl.a",
      "META-INF/native/macos/amd64/shared/libtcnative.dylib",
      "META-INF/native/macos/amd64/static/libtcnative.a",
      renameTo = ::renameNativeCryptoLib,
    )

    Platform.includedIn(Platform.DARWIN_AARCH64::class.java) -> access.unpackLibrary(
      TCNATIVE_JAR,
      "netty_tcnative_osx_aarch64",
      "aarch64",
      "META-INF/native/macos/aarch64/libapr-2.dylib",
      "META-INF/native/macos/aarch64/libapr-2.a",
      "META-INF/native/macos/aarch64/libcrypto.a",
      "META-INF/native/macos/aarch64/libdecrepit.a",
      "META-INF/native/macos/aarch64/libssl.a",
      "META-INF/native/macos/aarch64/shared/libtcnative.dylib",
      "META-INF/native/macos/aarch64/static/libtcnative.a",
      renameTo = ::renameNativeCryptoLib,
    )

    // no support on this platform
    else -> emptyList()
  }

  override fun nativeLibs(access: BeforeAnalysisAccess) = when (val arch = System.getProperty(ARCH_PROP)) {
    "x86_64", "amd64" -> "x86_64"
    "aarch64", "arm64" -> "aarch_64"
    else -> error("Unsupported architecture: $arch")
  }.let { archTag ->
    when (STATIC_JNI) {
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
            "netty_tcnative_osx_$archTag",
            *tcnative,
            builtin = true,
            initializer = true,
            absolutePath = Path(System.getProperty("elide.target")).resolve("libnetty_tcnative_osx_$archTag.a"),
          ),
          linux = libraryNamed(
            "netty_tcnative_linux_$archTag",
            *tcnative,
            builtin = true,
            initializer = true,
            absolutePath = Path(System.getProperty("elide.target")).resolve("libnetty_tcnative_linux_$archTag.a"),
          ),
        )
      )
    }
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    listOf(
      "apr-2",
      "crypto",
      "ssl",
      "sqlite3",
    ).forEach {
      (access as BeforeAnalysisAccessImpl).nativeLibraries.addStaticJniLibrary(it)
    }
  }
}
