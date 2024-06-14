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
import elide.runtime.feature.NativeLibraryFeature.UnpackedNative

/** Registers native library for Jansi. */
@VMFeature internal class NativeConsoleFeature : AbstractStaticNativeLibraryFeature() {
  companion object {
    private const val STATIC_JNI: Boolean = true
  }

  override fun getDescription(): String = "Registers native console libraries"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return (
      access.findClassByName("org.fusesource.jansi.AnsiConsole") != null
    )
  }

  override fun nativeLibs(access: BeforeAnalysisAccess) = listOf(
    libraryNamed(
      "terminal",
      "org.fusesource.jansi.internal.CLibrary",
      "org.fusesource_jansi_internal_Kernel32",
      "org.jline.nativ.JLineLibrary",
      "org.jline.nativ.CLibrary",
      builtin = STATIC_JNI,
    ),
  )

  override fun unpackNatives(access: BeforeAnalysisAccess): List<UnpackedNative> = if (STATIC_JNI) when {
    /* Static JNI */

    Platform.includedIn(Platform.LINUX_AMD64::class.java) -> access.unpackLibrary(
      "terminal",
      "terminal",
      "x86-64",
      "META-INF/native/linux/amd64/shared/libterminal.so",
      "META-INF/native/linux/amd64/static/libterminal.a",
    )

    Platform.includedIn(Platform.LINUX_AARCH64::class.java) -> access.unpackLibrary(
      "terminal",
      "terminal",
      "aarch64",
      "META-INF/native/linux/aarch64/shared/libterminal.so",
      "META-INF/native/linux/aarch64/static/libterminal.a",
    )

    Platform.includedIn(Platform.DARWIN_AMD64::class.java) -> access.unpackLibrary(
      "terminal",
      "terminal",
      "x86-64",
      "META-INF/native/macos/amd64/shared/libterminal.dylib",
      "META-INF/native/macos/amd64/static/libterminal.a",
    )

    Platform.includedIn(Platform.DARWIN_AARCH64::class.java) -> access.unpackLibrary(
      "terminal",
      "terminal",
      "aarch64",
      "META-INF/native/macos/aarch64/shared/libterminal.dylib",
      "META-INF/native/macos/aarch64/static/libterminal.a",
    )

    Platform.includedIn(Platform.WINDOWS_AMD64::class.java) -> access.unpackLibrary(
      "terminal",
      "terminal",
      "x86-64",
      "META-INF/native/windows/amd64/shared/terminal.dll",
      "META-INF/native/windows/amd64/static/terminal.lib",
    )

    Platform.includedIn(Platform.WINDOWS_AARCH64::class.java) -> access.unpackLibrary(
      "terminal",
      "terminal",
      "x86-64",
      "META-INF/native/windows/aarch64/shared/terminal.dll",
      "META-INF/native/windows/aarch64/static/terminal.lib",
    )

    else -> error("Unsupported OS: ${System.getProperty("os.name")}")
  } else when {
    /* Dynamic JNI */

    Platform.includedIn(Platform.LINUX::class.java) -> when (System.getProperty("os.arch")) {
      "x86_64", "amd64" -> access.unpackLibrary(
        "jansi",
        "jansi",
        "x86-64",
        "org/fusesource/jansi/internal/native/Linux/x86_64/libjansi.so",
      )

      "aarch64", "arm64" -> access.unpackLibrary(
        "jansi",
        "jansi",
        "aarch64",
        "org/fusesource/jansi/internal/native/Linux/arm64/libjansi.so",
      )

      else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
    }

    Platform.includedIn(Platform.MACOS::class.java) -> when (System.getProperty("os.arch")) {
      "x86_64", "amd64" -> access.unpackLibrary(
        "jansi",
        "jansi",
        "x86-64",
        "org/fusesource/jansi/internal/native/Mac/x86_64/libjansi.jnilib",
      )

      "aarch64", "arm64" -> access.unpackLibrary(
        "jansi",
        "jansi",
        "aarch64",
        "org/fusesource/jansi/internal/native/Mac/arm64/libjansi.jnilib",
      )

      else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
    }

    Platform.includedIn(Platform.WINDOWS::class.java) -> when (System.getProperty("os.arch")) {
      "x86_64", "amd64" -> access.unpackLibrary(
        "jansi",
        "jansi",
        "x86-64",
        "org/fusesource/jansi/internal/native/Windows/x86_64/libjansi.dll",
      )

      "aarch64", "arm64" -> access.unpackLibrary(
        "jansi",
        "jansi",
        "aarch64",
        "org/fusesource/jansi/internal/native/Windows/arm64/libjansi.dll",
      )

      else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
    }

    else -> error("Unsupported OS: ${System.getProperty("os.name")}")
  }
}
