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

import elide.core.api.Symbolic

// Constants used for image formats.
private const val IMG_TYPE_PNG = "png"
private const val IMG_TYPE_JPG = "jpg"
private const val IMG_TYPE_JPEG = "jpeg"
private const val IMG_TYPE_SVG = "svg"
private const val IMG_TYPE_GIF = "gif"
private const val IMG_TYPE_WEBP = "webp"
private const val IMG_TYPE_AVIF = "avif"

/**
 * ## Image Format
 *
 * Enumerates supported and/or recognized common computer image formats. These formats are used by the image processing
 * pipeline to recognize and handle images appropriately.
 *
 * @property symbol String at which this format is recognized. Also the primary extension for this format.
 * @property aliases Array of alternative extensions/names for this format.
 * @property optimizable Whether this format can be optimized by Elide.
 * @property encodable Whether this format can be converted to by Elide.
 */
public enum class ImageFormat (
  override val symbol: String,
  public val contentType: String = "image/$symbol",
  public val aliases: Array<String> = emptyArray(),
  public val optimizable: Boolean = false,
  public val encodable: Boolean = false,
) : Symbolic<String> {
  /** PNG format. */
  PNG(IMG_TYPE_PNG, optimizable = true, encodable = true),

  /** JPEG format. */
  JPG(IMG_TYPE_JPG, "image/$IMG_TYPE_JPEG", aliases = arrayOf(IMG_TYPE_JPEG), optimizable = true, encodable = true),

  /** SVG format. */
  SVG(IMG_TYPE_SVG, "image/svg+xml", optimizable = true),

  /** GIF format. */
  GIF(IMG_TYPE_GIF),

  /** WebP format. */
  WEBP(IMG_TYPE_WEBP, encodable = true),

  /** AVIF format. */
  AVIF(IMG_TYPE_AVIF, encodable = true);

  /** Tools for working with [ImageFormat] instances. */
  public companion object: Symbolic.SealedResolver<String, ImageFormat> {
    override fun resolve(symbol: String): ImageFormat = when (symbol) {
      IMG_TYPE_PNG -> PNG
      IMG_TYPE_JPG, IMG_TYPE_JPEG -> JPG
      IMG_TYPE_SVG -> SVG
      IMG_TYPE_GIF -> GIF
      IMG_TYPE_WEBP -> WEBP
      IMG_TYPE_AVIF -> AVIF
      else -> throw unresolved(symbol)
    }
  }
}
