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

package elide.http

/**
 * ## HTTP URL
 *
 * Specifies a URL type, and associated properties, to the extent needed by simple HTTP operations. The properties on
 * this URL are expected to be parsed on-demand unless otherwise noted. Where no value is available, sensible or
 * standards-compliant defaults are used.
 *
 * ### URL Properties
 *
 * - [scheme]: The URL scheme (e.g. `http`, `https`, etc.)
 * - [host]: The URL host (e.g. `example.com`)
 * - [port]: The URL port (e.g. `80`, `443`, etc.)
 * - [path]: The URL path (e.g. `/foo/bar`)
 * - [params]: The URL query parameters (e.g. `?foo=bar&baz=qux`)
 */
public sealed interface HttpUrl {
  /** HTTP URL scheme. */
  public val scheme: String

  /** HTTP URL host. */
  public val host: String

  /** HTTP URL port. */
  public val port: UShort

  /** HTTP URL path. */
  public val path: String

  /** HTTP URL params. */
  public val params: Params

  /** Platform implementation of an HTTP URL type. */
  public interface PlatformHttpUrl<T> : HttpUrl {
    /** Platform-specific URL value. */
    public val value: T & Any
  }
}
