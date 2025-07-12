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
package elide.tooling.web

/**
 * # Web Project
 *
 * Describes configuration for an Elide project which targets the web platform, for consumption in a browser; web
 * projects typically involve code in JavaScript, TypeScript, CSS, and HTML.
 */
public sealed interface WebProject {
  /**
   * Indicate whether this web project is dynamic (involving a server) or static (involving only inert files).
   *
   * @return `true` if this web project is dynamic, `false` if it is static.
   */
  public fun isDynamic(): Boolean

  /**
   * Indicate whether the project starts its own server when run; if not, Elide will start a server.
   *
   * @return `true` if this web project starts its own server, `false` if Elide will start a server for it.
   */
  public fun startsServer(): Boolean
}
