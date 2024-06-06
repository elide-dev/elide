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
import java.nio.file.Path
import elide.annotations.internal.VMFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.NativeLibraryFeature.UnpackedNative

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
  }

  override fun getDescription(): String = "Registers native transport libraries"

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return (
      access.findClassByName("io.netty.channel.kqueue.KQueue") != null ||
      access.findClassByName("io.netty.channel.epoll.Epoll") != null ||
      access.findClassByName("io.netty.incubator.channel.uring.IOUring") != null
    )
  }

  private fun transportLibV2(name: String, vararg impl: String): NativeLibInfo {
    return libraryNamed(
      name,
      *impl,
      builtin = true,
      initializer = true,
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
        linux = transportLibV2("netty_transport_native_epoll", *epollImpls),
        darwin = transportLibV2("netty_transport_native_kqueue", *kqueueImpls),
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

  override fun unpackNatives(access: BeforeAnalysisAccess): List<UnpackedNative> {
    if (enableV2Transport) when {
      Platform.includedIn(Platform.LINUX_AMD64::class.java) -> return access.unpackLibrary(
        "transport-epoll",
        "netty_transport_native_epoll",
        "x86-64",
        "META-INF/native/libtransport-epoll.a",  // @TODO: gradle doesn't support arm64 linux
        "META-INF/native/libtransport-epoll.so",
        renameTo = { "libnetty_transport_native_epoll.${it.substringAfterLast(".")}" },
      )

      Platform.includedIn(Platform.LINUX_AARCH64::class.java) -> return access.unpackLibrary(
        "transport-epoll",
        "netty_transport_native_epoll",
        "aarch64",
        "META-INF/native/libtransport-epoll.a",
        "META-INF/native/libtransport-epoll.so",
        renameTo = { "libnetty_transport_native_epoll.${it.substringAfterLast(".")}" },
      )

      Platform.includedIn(Platform.DARWIN_AMD64::class.java) -> return access.unpackLibrary(
        "transport-kqueue",
        "netty_transport_native_kqueue",
        "aarch64",
        "META-INF/native/x86-64/libtransport-kqueue.a",
        "META-INF/native/x86-64/libtransport-kqueue.dylib",
        renameTo = { "libnetty_transport_native_kqueue.${it.substringAfterLast(".")}" },
      )

      Platform.includedIn(Platform.DARWIN_AARCH64::class.java) -> return access.unpackLibrary(
        "transport-kqueue",
        "netty_transport_native_kqueue",
        "aarch64",
        "META-INF/native/arm64/libtransport-kqueue.a",
        "META-INF/native/arm64/libtransport-kqueue.dylib",
        renameTo = { "libnetty_transport_native_kqueue.${it.substringAfterLast(".")}" },
      )
    } else when {
      Platform.includedIn(Platform.LINUX::class.java) -> when (System.getProperty("os.arch")) {
        "x86_64", "amd64" -> return access.unpackLibrary(
          "netty-transport-native-epoll",
          "netty_transport_native_epoll_x86_64",
          "x86-64",
          "META-INF/native/libnetty_transport_native_epoll_x86_64.so",
        ) { io.netty.channel.epoll.Epoll.ensureAvailability() }

        "aarch64", "arm64" -> return access.unpackLibrary(
          "netty-transport-native-epoll",
          "netty_transport_native_epoll_aarch_64",
          "aarch64",
          "META-INF/native/libnetty_transport_native_epoll_aarch_64.so",
        ) { io.netty.channel.epoll.Epoll.ensureAvailability() }
      }

      Platform.includedIn(Platform.DARWIN::class.java) -> when (System.getProperty("os.arch")) {
        "x86_64", "amd64" -> return access.unpackLibrary(
          "netty-transport-native-kqueue",
          "netty_transport_native_kqueue_x86_64",
          "x86-64",
          "META-INF/native/libnetty_transport_native_kqueue_x86_64.jnilib",
        ) { io.netty.channel.kqueue.KQueue.ensureAvailability() }

        "aarch64", "arm64" -> return access.unpackLibrary(
          "netty-transport-native-kqueue",
          "netty_transport_native_kqueue_aarch_64",
          "aarch64",
          "META-INF/native/libnetty_transport_native_kqueue_aarch_64.jnilib",
        ) { io.netty.channel.kqueue.KQueue.ensureAvailability() }
      }
    }
    return emptyList()
  }
}
