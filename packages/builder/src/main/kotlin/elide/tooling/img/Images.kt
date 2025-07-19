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
import java.nio.file.Path
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import elide.tooling.img.ImageOptions.AvifOptions
import elide.tooling.img.ImageOptions.JpgOptions
import elide.tooling.img.ImageOptions.PngOptions
import elide.tooling.img.ImageOptions.WebpOptions

// Multiplier for buffer size allocation, to ensure we have enough space for image data.
private const val BUF_SIZE_FACTOR = 1.5

/**
 * # Images
 *
 * Provides static utilities for working with image media, particularly web-supported image formats such as PNG, JPEG,
 * WebP, AVIF, and others.
 *
 * Firstly, a source should be assembled which provides image data in some fashion (either via inline values, or a file
 * reference by path). This source can then be used to configure parameters and perform image processing, with data
 * provided back to the caller.
 */
public object Images {
  /** Load native code for image processing; does not need to be called explicitly. */
  public fun load() {
    ImgNative.load()
  }

  // Check a byte buffer for validity.
  private fun checkBuffer(buffer: ByteBuffer): ByteBuffer = buffer.apply {
    require(!isReadOnly) { "Cannot transform a read-only byte buffer" }
    require(capacity() > 0) { "Cannot transform an empty byte buffer" }
    require(isDirect) { "Cannot transform an non-direct byte buffer" }
  }

  /**
   * Communicates an image processing result.
   *
   * @property isSuccess Whether the operation was successful.
   */
  public sealed interface ImageResult<T> where T: ImageOptions {
    public val isSuccess: Boolean
  }

  /**
   * Carries image data as a result of a successful image operation.
   *
   * @property options Options used for the image operation.
   */
  public class ImageData<T: ImageOptions> internal constructor (
    public val options: T,
    public val result: ByteBuffer,
  ) : ImageResult<T> {
    override val isSuccess: Boolean get() = true
  }

  /**
   * Exception type for image operation errors.
   *
   * @property options Options used for the image operation.
   */
  public class ImgOperationError(
    public val options: ImageOptions,
    message: String,
    cause: Throwable? = null,
  ) : Exception(message, cause), ImageResult<ImageOptions> {
    override val isSuccess: Boolean get() = false
  }

  /**
   * Represents source material for an image operation.
   */
  public sealed interface ImageSource {
    /**
     * Provide image data.
     *
     * @return Byte array containing image data.
     */
    public fun provide(): ByteBuffer
  }

  /**
   * Provides image data from some callable.
   */
  public fun interface ImageSourceInMemory : ImageSource

  /**
   * Provides image data from some callable.
   */
  public fun interface ImageFile : ImageSource {
    override fun provide(): ByteBuffer = atPath().toFile().readBytes().let {
      ByteBuffer.allocateDirect(it.size).apply {
        put(it)
        flip()
      }
    }

    /** Emit the path to the image file. */
    public fun atPath(): Path
  }

  private fun checkOptions(options: ImageOptions): ImageOptions = options.apply {
    // check and prepare options
    val (isOk, optionsErr) = check()
    if (!isOk) {
      throw ImgOperationError(
        options = options,
        message = "Invalid image options: ${optionsErr ?: "unknown error"}",
      )
    }
  }

  /** Allocate a buffer sized for at least [size], applying the constant size factor for safety. */
  public fun imageBufferForSize(size: UInt): ByteBuffer = ByteBuffer.allocateDirect(
    (size.toDouble() * BUF_SIZE_FACTOR).roundToInt()
  )

  /** Allocate a buffer sized filled with the provided [bytes]. */
  public fun imageBufferFor(bytes: ByteArray): ByteBuffer = imageBufferForSize(bytes.size.toUInt()).apply {
    put(bytes)
    flip()
  }

  /**
   * Compress an image from a source to a target, using the provided options.
   *
   * Note: Compression of this kind is only supported for the following formats at this time:
   * - PNG
   * - JPEG
   *
   * @param options Options to use for compression (type-specific).
   * @param context Coroutine context to use for the operation; defaults to [Dispatchers.IO].
   * @param from Source image material to compress.
   * @return Result of the image operation, an [ImageData] instance containing the compressed image data.
   * @throws ImgOperationError If the operation fails for any reason, including invalid options.
   */
  @Suppress("UNCHECKED_CAST")
  public suspend fun <T: ImageOptions> compress(
    options: T,
    context: CoroutineContext = Dispatchers.IO,
    from: ImageSource,
  ): ImageData<T> = withContext(context) {
    assert(options.format.optimizable) { "Image format '${options.format.name}' is not optimizable" }
    load()

    // perform compression
    when (val opts = checkOptions(options)) {
      is PngOptions -> ImgNative.compressInPlace(opts) { checkBuffer(from.provide()) }
      is JpgOptions -> ImgNative.compressInPlace(opts) { checkBuffer(from.provide()) }
      else -> error("Format is not supported for compression: ${options.format}")
    }.let { result ->
      when (val inner = result) {
        is ImgOperationError -> throw inner
        is ImageData<*> -> inner as ImageData<T>
      }
    }
  }

  /**
   * Convert (and compress) an image from a source to a target, using the provided options.
   *
   * Note: Conversion of this kind is only supported for the following formats at this time:
   * - AVIF
   * - WebP
   * - PNG
   * - JPEG
   *
   * @param options Options to use for compression (type-specific).
   * @param context Coroutine context to use for the operation; defaults to [Dispatchers.IO].
   * @param from Source image material to convert.
   * @return Result of the image operation, an [ImageData] instance containing the compressed image data.
   * @throws ImgOperationError If the operation fails for any reason, including invalid options.
   */
  @Suppress("UNCHECKED_CAST")
  public suspend fun <T: ImageOptions> convert(
    name: String,
    options: T,
    context: CoroutineContext = Dispatchers.IO,
    from: ImageSource,
  ): ImageData<T> = withContext(context) {
    assert(options.format.encodable) { "Image format '${options.format.name}' is not encodable" }
    load()

    // perform conversion
    when (val opts = checkOptions(options)) {
      is PngOptions -> return@withContext compress<PngOptions>(opts, context, from) as ImageData<T>
      is JpgOptions -> return@withContext compress<JpgOptions>(opts, context, from) as ImageData<T>
      is WebpOptions -> ImgNative.convertInPlace(opts) { checkBuffer(from.provide()) }
      is AvifOptions -> ImgNative.convertInPlace(opts) { checkBuffer(from.provide()) }
    }.let { result ->
      when (val inner = result) {
        is ImgOperationError -> throw inner
        is ImageData<*> -> inner as ImageData<T>
      }
    }
  }
}
