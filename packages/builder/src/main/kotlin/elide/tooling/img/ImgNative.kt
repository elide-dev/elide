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
package elide.tooling.img

import org.graalvm.nativeimage.ImageInfo
import java.nio.ByteBuffer
import kotlinx.atomicfu.atomic
import kotlin.jvm.Throws
import elide.runtime.core.lib.NativeLibraries
import elide.tooling.img.ImageOptions.*
import elide.tooling.img.Images.ImageData
import elide.tooling.img.Images.ImageResult
import elide.tooling.img.Images.ImgOperationError

// JNI methods used for image processing.
private const val JNI_METHOD_COMPRESS_PNG = "compress_png"
private const val JNI_METHOD_COMPRESS_JPG = "compress_jpg"
private const val JNI_METHOD_CONVERT_TO_WEBP = "convert_to_webp"
private const val JNI_METHOD_CONVERT_TO_AVIF = "convert_to_avif"

// Implements native image compression and transformation methods via JNI. Note that this has nothing to do with the
// GraalVM-related concept of "native images," which relate to program binaries instead of visual image formats.
internal object ImgNative {
  private const val NATIVE_MEDIA_LIB = "media"
  private val initialized = atomic(false)

  // Load the native library for CSS parsing and building.
  @JvmStatic fun load(): Boolean = when (ImageInfo.inImageRuntimeCode()) {
    true -> true // built-in if running in native mode
    else -> when (initialized.value) {
      true -> true // already initialized
      false -> synchronized(this) {
        NativeLibraries.loadLibrary(NATIVE_MEDIA_LIB).also {
          when (it) {
            true -> initialized.value = true
            else -> error("Failed to load lib$NATIVE_MEDIA_LIB")
          }
        }
      }
    }
  }

  // Execute an image operation with the provided options and data provider, handling errors and returning the result.
  private inline fun <reified T: ImageOptions> exec(
    opts: T,
    provider: () -> ByteBuffer,
    crossinline op: (ByteBuffer) -> Boolean,
  ): ImageResult<T> {
    val data = provider.invoke()
    val op = runCatching {
      check(op.invoke(data)) {
        "Image operation failed for options: $opts"
      }
    }

    return when (op.isSuccess) {
      true -> ImageData(opts, data)
      false -> throw ImgOperationError(
        opts,
        "Failed to execute image operation: ${op.exceptionOrNull()?.message ?: "Unknown error"}",
        op.exceptionOrNull()
      )
    }
  }

  // Compress the provided data as PNG, using the provided options.2
  @Throws(ImgOperationError::class)
  fun compressInPlace(options: PngOptions, dataProvider: () -> ByteBuffer) = exec(options, dataProvider) { data ->
    compressPng(options, data)
  }

  // Compress the provided data as JPG, using the provided options.
  @Throws(ImgOperationError::class)
  fun compressInPlace(options: JpgOptions, dataProvider: () -> ByteBuffer) = exec(options, dataProvider) { data ->
    compressJpg(options, data)
  }

  // Convert the provided data to WebP format, using the provided options.
  @Throws(ImgOperationError::class)
  fun convertInPlace(options: WebpOptions, dataProvider: () -> ByteBuffer) = exec(options, dataProvider) { data ->
    convertToWebP(options, data)
  }

  // Convert the provided data to AVIF format, using the provided options.
  @Throws(ImgOperationError::class)
  fun convertInPlace(options: AvifOptions, dataProvider: () -> ByteBuffer) = exec(options, dataProvider) { data ->
    convertToAvif(options, data)
  }

  // Native JNI method to compress PNG data.
  @JvmName(JNI_METHOD_COMPRESS_PNG)
  private external fun compressPng(options: PngOptions?, data: ByteBuffer): Boolean

  // Native JNI method to compress JPG data.
  @JvmName(JNI_METHOD_COMPRESS_JPG)
  private external fun compressJpg(options: JpgOptions?, data: ByteBuffer): Boolean

  // Native JNI method to convert data to WebP format.
  @JvmName(JNI_METHOD_CONVERT_TO_WEBP)
  private external fun convertToWebP(options: WebpOptions?, data: ByteBuffer): Boolean

  // Native JNI method to convert data to AVIF format.
  @JvmName(JNI_METHOD_CONVERT_TO_AVIF)
  private external fun convertToAvif(options: AvifOptions?, data: ByteBuffer): Boolean
}
