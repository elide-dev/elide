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

import org.graalvm.nativeimage.Platform
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import java.nio.file.Path
import elide.annotations.engine.VMFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo

/** Registers native transport libraries for static JNI. */
@VMFeature internal class NativeTransportFeature : AbstractStaticNativeLibraryFeature() {
  companion object {
    const val ENABLED = true

    // Whether to enable V2 transport libraries.
    private val enableV2Transport = System.getProperty("elide.nativeTransport.v2") == "true"

    private val kqueueImpls = arrayOf(
      "io.netty.channel.kqueue.Native",
      "io.netty.channel.kqueue.KQueueStaticallyReferencedJniMethods",
    )

    private val epollImpls = arrayOf(
      "io.netty.channel.epoll.Native",
      "io.netty.channel.epoll.NativeStaticallyReferencedJniMethods",
    )

    private val iouringImpls = arrayOf(
      "io.netty.incubator.channel.uring.Native",
      "io.netty.incubator.channel.uring.LinuxSocket",
      "io.netty.incubator.channel.uring.NativeStaticallyReferencedJniMethods",
    )
  }

  override fun getDescription(): String = "Registers native transport libraries"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return (
      access.findClassByName("io.netty.channel.kqueue.KQueue") != null ||
      access.findClassByName("io.netty.channel.epoll.Epoll") != null ||
      access.findClassByName("io.netty.incubator.channel.uring.IOUring") != null
    )
  }

  private fun transportLibV2(impl: Array<String>): NativeLibInfo {
    return libraryNamed(
      "umbrella",
      *impl,
      builtin = true,
    )
  }

  private fun transportLibNamed(name: String, arch: String, vararg impl: String, path: Path? = null): NativeLibInfo {
    val archToken = arch.replace("-", "_").replace("aarch64", "aarch_64")
    return libraryNamed(
      "netty_transport_native_${name}_${archToken}",
      *impl,
      builtin = false,
      eager = false,
      initializer = true,
      absolutePath = path,
    )
  }

  override fun nativeLibs(access: BeforeAnalysisAccess) = if (ENABLED) when {
    enableV2Transport -> listOfNotNull(
      nativeLibrary(
        linux = transportLibV2(epollImpls.plus(iouringImpls)),
        darwin = transportLibV2(kqueueImpls),
      )
    )

    else -> listOfNotNull(
      // Native Transport: Linux
      if (!Platform.includedIn(Platform.LINUX::class.java)) null else when {
        Platform.includedIn(Platform.AMD64::class.java) ->
          nativeLibrary(linux = transportLibNamed("epoll", "x86-64", *epollImpls))
        Platform.includedIn(Platform.AARCH64::class.java) ->
          nativeLibrary(linux = transportLibNamed("epoll", "aarch64", *epollImpls))
        else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
      },

      // quiche
      if (!Platform.includedIn(Platform.LINUX::class.java)) null else
        nativeLibrary(linux = libraryNamed("netty_quiche_linux")),

      // Native Transport: Darwin
      if (!Platform.includedIn(Platform.DARWIN::class.java)) null else when {
        Platform.includedIn(Platform.AMD64::class.java) ->
          nativeLibrary(darwin = transportLibNamed("kqueue", "x86-64", *kqueueImpls))
        Platform.includedIn(Platform.AARCH64::class.java) ->
          nativeLibrary(darwin = transportLibNamed("kqueue", "aarch64", *kqueueImpls))
        else -> error("Unsupported architecture: ${System.getProperty("os.arch")}")
      },

      // native dns
      if (!Platform.includedIn(Platform.LINUX::class.java)) null else
        nativeLibrary(darwin = libraryNamed("netty_resolver_dns_native_macos")),

      // quiche
      if (!Platform.includedIn(Platform.LINUX::class.java)) null else
        nativeLibrary(darwin = libraryNamed("netty_quiche_osx")),
    )
  } else emptyList()
}
