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
package elide.tooling.web.mdx

import elide.runtime.diag.Diagnostics
import elide.tooling.md.Markdown
import elide.tooling.md.Markdown.MarkdownOptions

/**
 * ## MDX Builder
 *
 * Extends the default [Markdown] builder with support for MDX; such support is handled natively, so we call into the
 * Rust layer via the `mdxjs` crate to render instead.
 *
 * MDX is a combination of Markdown and React-like syntaxes. Components can be imported and used from Markdown, and then
 * JavaScript is rendered in React/JSX to fulfill the input MDX, rather than generating pure HTML as normal Markdown
 * typically does.
 */
public object MdxBuilder {
  @Suppress("UNUSED_PARAMETER")
  @JvmStatic public fun renderMdx(src: String, options: MarkdownOptions = MarkdownOptions.defaults()): String {
    return MdxNative.buildMdx(src) ?: Diagnostics.query(true) { true }.toList().let { diags ->
      error("MDX compile failed with diagnostics: ${diags.joinToString("\n") { it.formattedString().toString() }}")
    }
  }
}
