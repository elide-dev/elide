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
package elide.tooling.project.agents

import kotlinx.serialization.Serializable

/**
 * ### Built-in Elide Advice
 *
 * Builds [AgentAdvice] for Elide itself, which can be held with project advice in AI context files.
 */
@Serializable public data object ElideAdvice : AdviceEnabled {
  override fun advice(ctx: AgentAdviceBuilder) {
    ctx.apply {
      section(heading("Elide Runtime"), text {
        """
      Elide is a Node.js-like runtime, which leverage GraalVM for polyglot execution of JavaScript, TypeScript, Python,
      JVM, and other languages. Elide is used similar to Node.js, with common commands being:

      - `elide install`: Install dependencies from ecosystems like Maven, NPM, or PyPI.
      - `elide build`: Build the project, which involves compiling code, building output artifacts/containers, etc.
      - `elide test`: Run tests defined in the project, similar to how you would with `npm test` or `mvn test`.
      - `elide run <script>`: Run a script in the Elide runtime.
      - `elide lsp`: Start the Language Server Protocol (LSP) server for IDE integration.
      - `elide mcp`: Start the Multi-Context Protocol (MCP) server for polyglot execution and interaction.

      See `elide --help` for a full list of commands and options. Note that `<script>` in `elide run <script>` can be
      several things, including:

      - A source file to run
      - A script defined in `elide.pkl` in the `scripts` block
      - A script defined in a foreign manifest, like `package.json`
      - The name of a binary defined in the project's dependencies, for example, within `node_modules/.bin/`

      Some other notes about Elide:
      - Elide can run TypeScript or TSX/JSX files directly, without a build step
      - Elide is similar to Node.js and supports APIs like `node:fs`, `node:util`, etc., but not all of them
      - Elide supports polyglot execution, end-to-end building and testing, LSP, MCP, and more; see `elide help`
      - Consult https://docs.elide.dev, https://github.com/elide-dev/elide, or `elide help` for more information

      This software project uses Elide.
      """
      })
    }
  }
}
