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

@file:Suppress("UrlHashCode", "SameParameterValue")

package elide.tool.engine

import io.netty.util.internal.NativeLibraryLoader
import io.netty.util.internal.PlatformDependent
import io.netty.util.internal.ThrowableUtil
import java.io.*
import java.net.URL
import java.util.*
import elide.runtime.Logger
import elide.tool.cli.Statics

/** Utilities for loading and copied native libraries, inspired by Netty. */
internal object NativeUtil {
  // Logger to use for warnings when unpacking native support libraries.
  @JvmStatic private val logger: Logger by lazy {
    Statics.logging
  }

  // Close an IO resource quietly, ignoring any exceptions.
  @JvmStatic private fun closeQuietly(c: Closeable?) {
    if (c != null) {
      try {
        c.close()
      } catch (ignore: IOException) {
        // ignore
      }
    }
  }

  // Calculate a mangled package prefix for Netty natives in a shaded environment.
  @JvmStatic private fun calculateMangledPackagePrefix(): String {
    val maybeShaded = NativeLibraryLoader::class.java.getName()
    // Use ! instead of . to avoid shading utilities from modifying the string
    val expected = "io!netty!util!internal!NativeLibraryLoader".replace('!', '.')
    if (!maybeShaded.endsWith(expected)) {
      throw UnsatisfiedLinkError(
        String.format(
          "Could not find prefix added to %s to get %s. When shading, only adding a "
                  + "package prefix is supported",
          expected, maybeShaded,
        ),
      )
    }
    return maybeShaded.substring(0, maybeShaded.length - expected.length)
      .replace("_", "_1")
      .replace('.', '_')
  }

  // Calculate/retrieve a URL for an embedded native library resource.
  @JvmStatic private fun getResource(path: String, loader: ClassLoader?): URL? {
    val urls: Enumeration<URL> = try {
      if (loader == null) {
        ClassLoader.getSystemResources(path)
      } else {
        loader.getResources(path)
      }
    } catch (iox: IOException) {
      throw RuntimeException("An error occurred while getting the resources for $path", iox)
    }
    val urlsList: List<URL> = Collections.list(urls)

    return when (urlsList.size) {
      0 -> null
      1 -> urlsList[0]
      else -> null  // ambiguity; pass on to netty to handle
    }
  }

  // Try to load the native library named `libName`.
  @JvmStatic private fun loadNativeLibrary(libName: String, absolute: Boolean) {
    if (absolute) {
      System.load(libName)
    } else {
      System.loadLibrary(libName)
    }
  }

  @JvmStatic private fun loadLibrary(name: String, absolute: Boolean): Boolean {
    var suppressed: Throwable? = null
    return try {
      suppressed = try {
        loadNativeLibrary(name, absolute) // Fallback to local helper class.
        logger.debug("Successfully loaded the library {}", name)
        null
      } catch (nsme: NoSuchMethodError) {
        nsme
      } catch (ule: UnsatisfiedLinkError) {
        ule
      }
      return suppressed == null
    } catch (nsme: NoSuchMethodError) {
      ThrowableUtil.addSuppressed(nsme, suppressed)
      false
    } catch (ule: UnsatisfiedLinkError) {
      ThrowableUtil.addSuppressed(ule, suppressed)
      throw ule
    }
  }

  @JvmStatic internal fun loadOrCopy(
    workdir: File,
    path: String,
    libName: String,
    loader: ClassLoader,
    forceCopy: Boolean = false,
    forceLoad: Boolean = false,
    loadFromPath: Boolean = true,
  ): Pair<Boolean, Boolean> {
    // calculate naming info first
    val libname = System.mapLibraryName(libName)
    val index = libname.lastIndexOf('.')
    val prefix = libname.substring(0, index)
    val suffix = libname.substring(index)
    val libTarget = workdir.resolve(prefix + suffix)

    // unless we are force-copying, we can skip remaining logic if the file already exists
    if (!forceCopy && libTarget.exists()) {
      return if (forceLoad) {
        if (loadFromPath) {
          try {
            loadNativeLibrary(libName, false)
            return true to false
          } catch (thr: Throwable) {
            // ignore
            logger.debug("Failed to load system library TEMP")
          }
        }
        loadNativeLibrary(libTarget.absolutePath, true)
        true to false
      } else {
        false to true
      }
    }

    val mangledPackagePrefix = calculateMangledPackagePrefix()
    val mangledName = mangledPackagePrefix + libName
    val suppressed: MutableList<Throwable> = ArrayList()
    try {
      // first try to load from java.library.path
      if (loadLibrary(mangledName, false)) {
        return false to true
      }
    } catch (ex: Throwable) {
      suppressed.add(ex)
    }

    var `in`: InputStream? = null
    var out: OutputStream? = null
    var url = getResource(path, loader)

    try {
      if (url == null) {
        if (PlatformDependent.isOsx()) {
          url = getResource(path.substringBeforeLast("/") + libname, loader) ?: run {
            val fnf = FileNotFoundException(path + libname)
            ThrowableUtil.addSuppressedAndClear(fnf, suppressed)
            throw fnf
          }
        } else {
          val fnf = FileNotFoundException(libname)
          ThrowableUtil.addSuppressedAndClear(fnf, suppressed)
          throw fnf
        }
      }

      `in` = url.openStream()
      out = FileOutputStream(libTarget)
      val buffer = ByteArray(8192)
      var length: Int
      while (`in`.read(buffer).also { length = it } > 0) {
        out.write(buffer, 0, length)
      }
      out.flush()

      // Close the output stream before loading the unpacked library,
      // because otherwise Windows will refuse to load it when it's in use by other process.
      closeQuietly(out)
      out = null

      return if (forceLoad) {
        loadNativeLibrary(libTarget.absolutePath, true)
        true to true
      } else {
        false to true
      }
    } catch (e: UnsatisfiedLinkError) {
      // Re-throw to fail the load
      ThrowableUtil.addSuppressedAndClear(e, suppressed)
      throw e
    } catch (e: Exception) {
      val ule = UnsatisfiedLinkError("could not load a native library: $path$libName")
      ule.initCause(e)
      ThrowableUtil.addSuppressedAndClear(ule, suppressed)
      throw ule
    } finally {
      closeQuietly(`in`)
      closeQuietly(out)
    }
  }
}
