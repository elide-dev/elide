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

import java.nio.ByteBuffer
import kotlin.jvm.Throws
import elide.tooling.img.ImageOptions.*
import elide.tooling.img.Images.ImageData
import elide.tooling.img.Images.ImageResult
import elide.tooling.img.Images.ImgOperationError
import elide.tooling.web.WebBuilder

// JNI methods used for image processing.
private const val JNI_METHOD_COMPRESS_PNG = "compressPng"
private const val JNI_METHOD_COMPRESS_JPG = "compressJpg"
private const val JNI_METHOD_CONVERT_TO_WEBP = "convertToWebp"
private const val JNI_METHOD_CONVERT_TO_AVIF = "convertToAvif"

// Implements native image compression and transformation methods via JNI. Note that this has nothing to do with the
// GraalVM-related concept of "native images," which relate to program binaries instead of visual image formats.
internal object ImgNative {
  // Load the native library for CSS parsing and building.
  @JvmStatic fun load(): Boolean = WebBuilder.load()

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
      false -> op.exceptionOrNull().let { exc ->
        val excLabel = buildString {
          if (exc != null) {
            append(exc::class.java.name)
            append(" ")
            append(exc.message ?: "Unknown error")
          } else {
            append("Unknown error")
          }
        }
        throw ImgOperationError(
          opts,
          "Failed to execute image operation: $excLabel",
          op.exceptionOrNull()
        )
      }
    }
  }

  // Compress the provided data as PNG, using the provided options.2
  @Throws(ImgOperationError::class)
  fun compressInPlace(options: PngOptions, dataProvider: () -> ByteBuffer) = exec(options, dataProvider) { data ->
    assert(compressPng()) { "Compress PNG returned false" }
    true
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
  @JvmStatic private external fun compressPng(): Boolean

  // Native JNI method to compress JPG data.
  @JvmName(JNI_METHOD_COMPRESS_JPG)
  @JvmStatic private external fun compressJpg(options: JpgOptions?, data: ByteBuffer?): Boolean

  // Native JNI method to convert data to WebP format.
  @JvmName(JNI_METHOD_CONVERT_TO_WEBP)
  @JvmStatic private external fun convertToWebP(options: WebpOptions?, data: ByteBuffer?): Boolean

  // Native JNI method to convert data to AVIF format.
  @JvmName(JNI_METHOD_CONVERT_TO_AVIF)
  @JvmStatic private external fun convertToAvif(options: AvifOptions?, data: ByteBuffer?): Boolean
}
