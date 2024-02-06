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

package elide.embedded.env

/**
 * # Environment Resolver
 *
 * Responsible for resolving environment-provided values, typically from system environment variables, or from JVM
 * system properties; during testing, this interface is mocked.
 */
public interface EnvResolver {
  /**
   * Resolves a system environment variable.
   *
   * @param key The key to resolve.
   * @return The resolved value, or `null` if not found.
   */
  public fun resolveEnv(key: EnvVariableName): String?

  /**
   * Resolves a system property.
   *
   * @param key The key to resolve.
   * @return The resolved value, or `null` if not found.
   */
  public fun resolveProperty(key: SystemPropertyName): String?

  /**
   * Resolves a value from a pair of [EnvVariableName] and [SystemPropertyName]; environment variables always win, if
   * both values are found.
   *
   * @param keys The pair of keys to resolve.
   * @return The resolved value, or `null` if not found.
   */
  public fun resolve(keys: Pair<EnvVariableName, SystemPropertyName>): String? {
    return resolveEnv(keys.first) ?: resolveProperty(keys.second)
  }
}
