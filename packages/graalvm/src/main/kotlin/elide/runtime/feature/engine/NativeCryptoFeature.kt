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

import org.graalvm.nativeimage.Platform
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import elide.annotations.internal.VMFeature

/** Registers native crypto libraries for static JNI. */
@VMFeature internal class NativeCryptoFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private const val PROVIDED = "provided"
  }

  override fun getDescription(): String = "Registers native crypto libraries"

  override fun isInConfiguration(access: IsInConfigurationAccess?): Boolean {
    return false
  }

  private fun initializeTcNative() {
    io.netty.handler.ssl.OpenSsl.ensureAvailability()
    io.netty.internal.tcnative.Library.initialize(PROVIDED, null)
  }

  override fun unpackNatives(access: BeforeAnalysisAccess) {
    when {
      Platform.includedIn(Platform.LINUX::class.java) -> when (System.getProperty("os.arch")) {
        "x86_64", "amd64" -> access.unpackLibrary(
          "netty-tcnative-boringssl-static",
          "netty_tcnative_linux_x86_64",
          "x86-64",
          "META-INF/native/libnetty_tcnative_linux_x86_64.so",
        ) { initializeTcNative() }

        "aarch64", "arm64" -> access.unpackLibrary(
          "netty-tcnative-boringssl-static",
          "netty_tcnative_linux_aarch_64",
          "aarch64",
          "META-INF/native/libnetty_tcnative_linux_aarch_64.so",
        ) { initializeTcNative() }
      }

      Platform.includedIn(Platform.DARWIN::class.java) -> when (System.getProperty("os.arch")) {
        "x86_64", "amd64" -> access.unpackLibrary(
          "netty-tcnative-boringssl-static",
          "netty_tcnative_osx_x86_64",
          "x86-64",
          "META-INF/native/libnetty_tcnative_osx_x86_64.jnilib",
        ) { initializeTcNative() }

        "aarch64", "arm64" -> access.unpackLibrary(
          "netty-tcnative-boringssl-static",
          "netty_tcnative_osx_aarch_64",
          "aarch64",
          "META-INF/native/libnetty_tcnative_osx_aarch_64.jnilib",
        ) { initializeTcNative() }
      }
    }
  }

  override fun nativeLibs(access: BeforeAnalysisAccess) = when (val arch = System.getProperty("os.arch")) {
    "x86_64", "amd64" -> "x86_64"
    "aarch64", "arm64" -> "aarch_64"
    else -> error("Unsupported architecture: $arch")
  }.let { archTag ->
    listOf(
      nativeLibrary(
        darwin = libraryNamed(
          "netty_tcnative_osx_$archTag",
          "io.netty.handler.ssl.OpenSsl",
          builtin = false,
          eager = false,
        ),
        linux = libraryNamed(
          "netty_tcnative_linux_$archTag",
          "io.netty.handler.ssl.OpenSsl",
          builtin = false,
          eager = false,
        ),
      )
    )
  }
}
