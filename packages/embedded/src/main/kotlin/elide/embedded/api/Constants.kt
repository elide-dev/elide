/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.api

/**
 * Constant values provided for public use.
 */
public data object Constants {
  /** Active/default API version. */
  public const val API_VERSION: String = "v1alpha1"

  /** Constants used as system property names. */
  public data object SystemProperty {
    public const val DATACENTER: String = "elide.datacenter"
    public const val REGION: String = "elide.region"
  }

  /** Constants used as environment variable names. */
  public data object Environment {
    /** Environment variable: datacenter. */
    public const val DATACENTER: String = "ELIDE_DATACENTER"

    /** Environment variable: region. */
    public const val REGION: String = "ELIDE_REGION"
  }

  /** Default values for key properties. */
  public data object Defaults {
    /** Default value if no datacenter property is specified. */
    public const val DATACENTER: String = "dc1"

    /** Default value if no region property is specified. */
    public const val REGION: String = "global"

    /** Engine defaults. */
    public data object Engine {
      /** Whether to run in isolates. */
      public const val SPAWN_ISOLATE: Boolean = false

      /** Default maximum isolate memory. */
      public const val MAX_ISOLATE_MEMORY: String = "256MB"

      /** Whether JIT compilation is enabled. */
      public const val COMPILATION: Boolean = true

      /** Compile in background threads. */
      public const val COMPILE_BACKGROUND: Boolean = true

      /** Untrusted code mitigation mode. */
      public const val UNTRUSTED_CODE_MITIGATION: String = "software"
    }
  }

  /** Constant flag names. */
  public data object Flag {
    /** Maximum isolate memory. */
    public const val MAX_ISOLATE_MEMORY: String = "engine.MaxIsolateMemory"

    /** Compilation setting. */
    public const val COMPILATION: String = "engine.Compilation"

    /** Compile in background threads. */
    public const val COMPILE_BACKGROUND: String = "engine.BackgroundCompilation"

    /** Whether to run in isolates. */
    public const val SPAWN_ISOLATE: String = "engine.SpawnIsolate"

    /** Untrusted code mitigations. */
    public const val UNTRUSTED_CODE_MITIGATION: String = "engine.UntrustedCodeMitigation"
  }
}
