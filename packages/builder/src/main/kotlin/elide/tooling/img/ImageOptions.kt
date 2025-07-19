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

/**
 * ## Image Options
 *
 * Sealed hierarchy of options, each of which implements the settings inherent to a given [ImageFormat]; concrete
 * options objects are created directly through public construction.
 */
public sealed interface ImageOptions {
  /** Whether to emit debug logs when working with this format or operation. */
  public val debug: Boolean

  /** The image format for which these options apply. */
  public val format: ImageFormat

  /** Check the options for validity, returning a pair of a boolean, and an optional error message. */
  public fun check(): Pair<Boolean, String?> = true to null

  /** Options for PNG images. */
  @JvmRecord public data class PngOptions(override val debug: Boolean = false): ImageOptions {
    override val format: ImageFormat get() = ImageFormat.PNG
  }

  /** Options for JPEG images. */
  @JvmRecord public data class JpgOptions(override val debug: Boolean = false): ImageOptions {
    override val format: ImageFormat get() = ImageFormat.JPG
  }

  /** Options for WebP images. */
  @JvmRecord public data class WebpOptions(override val debug: Boolean = false): ImageOptions {
    override val format: ImageFormat get() = ImageFormat.WEBP
  }

  /** Options for AVIF images. */
  @JvmRecord public data class AvifOptions(override val debug: Boolean = false): ImageOptions {
    override val format: ImageFormat get() = ImageFormat.AVIF
  }
}
