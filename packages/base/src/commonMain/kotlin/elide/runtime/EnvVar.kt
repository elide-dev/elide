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

package elide.runtime

import java.util.function.Supplier

/** Describes an environment variable, resolved at runtime, with contextual information about its source and purpose. */
public sealed interface EnvVar {
  /**
   * Name for the environment variable.
   */
  public val name: String

  /**
   * Value for the environment variable; if evaluated to `null`, the variable is skipped.
   */
  public val value: String?

  /**
   * Indicate whether a value can be resolved for this env var.
   */
  public val isPresent: Boolean get() = value != null

  /**
   * Source type for this environment variable; governs how a value is resolved.
   */
  public val source: EnvVariableSource

  /** Maps an inline (explicitly-provided) environment variable. */
  @JvmRecord public data class InlineEnvVar(
    override val name: String,
    override val value: String?,
  ) : EnvVar {
    override val source: EnvVariableSource get() = EnvVariableSource.INLINE
  }

  /** Maps a supplied (function-resolved) environment variable. */
  @JvmRecord public data class SuppliedEnvVar(
    override val name: String,
    private val supplier: Supplier<String?>,
  ) : EnvVar {
    override val source: EnvVariableSource get() = EnvVariableSource.INLINE
    override val value: String? get() = supplier.get()
  }

  /** Maps an environment variable which originates from a `.env` file. */
  @JvmRecord public data class DotEnvVar(
    public val file: String,
    override val name: String,
    override val value: String?,
  ) : EnvVar {
    override val source: EnvVariableSource get() = EnvVariableSource.INLINE
  }

  /** Maps an environment variable which uses a host environment variable (usually, at the same name). */
  @JvmRecord public data class HostMappedVar(
    public val mapped: String,
    public val defaultValue: String? = null,
    override val name: String,
  ) : EnvVar {
    override val source: EnvVariableSource get() = EnvVariableSource.HOST
    override val value: String? get() = System.getenv(name) ?: defaultValue
  }

  public companion object {
    /** @return [EnvVar] which describes an explicit variable (type [EnvVariableSource.INLINE]). */
    @JvmStatic public fun of(name: String, value: String): EnvVar = InlineEnvVar(name, value)

    /** @return [EnvVar] which describes a function-resolved variable (type [EnvVariableSource.INLINE]). */
    @JvmStatic public fun provide(name: String, provider: Supplier<String?>): EnvVar =
      SuppliedEnvVar(name, provider)

    /** @return [EnvVar] which describes a dotenv-resolved variable (type [EnvVariableSource.DOTENV]). */
    @JvmStatic public fun fromDotenv(file: String, name: String, value: String?): EnvVar =
      DotEnvVar(file, name, value)

    /** @return [EnvVar] which describes a host-resolved variable (type [EnvVariableSource.HOST]). */
    @JvmStatic public fun mapToHost(name: String, atName: String = name, defaultValue: String? = null): EnvVar =
      HostMappedVar(mapped = name, name = atName, defaultValue = defaultValue)
  }
}
