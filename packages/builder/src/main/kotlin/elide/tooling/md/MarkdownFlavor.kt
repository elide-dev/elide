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
package elide.tooling.md

/**
 * ## Markdown Flavor
 *
 * Enumerates supported or understood Markdown flavors.
 */
public enum class MarkdownFlavor {
  /**
   * Standard CommonMark Markdown.
   */
  CommonMark,

  /**
   * GitHub Flavored Markdown, which is a superset of CommonMark with additional features.
   */
  GitHub,

  /**
   * MDX, which is a superset of CommonMark with additional features for extended Markdown syntax via React.
   */
  Mdx,
}
