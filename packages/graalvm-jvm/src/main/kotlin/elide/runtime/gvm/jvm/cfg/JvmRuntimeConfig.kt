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

package elide.runtime.gvm.jvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.*
import elide.runtime.gvm.cfg.GuestRuntimeConfiguration
import elide.runtime.gvm.cfg.JsRuntimeConfig

/**
 * TBD.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.jvm")
public class JvmRuntimeConfig : Toggleable, GuestRuntimeConfiguration {
  public companion object {
    /** Default enablement of the JS VM. */
    public const val DEFAULT_ENABLED: Boolean = true

    /** Default character set to use with raw data exchanged with the JS VM. */
    public val DEFAULT_CHARSET: Charset = StandardCharsets.UTF_8

    /** Default JS VM locale. */
    public val DEFAULT_LOCALE: Locale = Locale.getDefault()

    /** Default JS VM time zone. */
    public val DEFAULT_TIMEZONE: ZoneId = ZoneId.systemDefault()
  }

  override fun isEnabled(): Boolean = DEFAULT_ENABLED

  /**
   * @return Default locale to apply to the JS VM. Defaults to the system default.
   */
  public val defaultLocale: Locale? get() = JsRuntimeConfig.DEFAULT_LOCALE

  /**
   * @return Default timezone to apply to the JS VM. Defaults to the system default.
   */
  public val timezone: ZoneId? get() = JsRuntimeConfig.DEFAULT_TIMEZONE

  /**
   * @return Default character set to apply when exchanging raw data with the JS VM. Defaults to `UTF-8`. `UTF-8` and
   *   `UTF-32` are explicitly supported; other support may vary.
   */
  public val charset: Charset? get() = null

  /**
   * @return Locale to use for embedded JS VMs.
   */
  public val locale: Locale? get() = null
}
