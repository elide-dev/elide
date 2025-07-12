## Project Advice

There is an Elide Project configuration file present for this project (`elide.pkl`).
Project configuration summary:

- Project Name: `elide`
- Version: `1.0.0-beta7`
- Description: `Polyglot runtime and toolchain`

### Project Scripts

The project has the following scripts defined, all usable with `elide <script>` or `elide run <script>`:

- `quicktest`: `elide test --coverage --coverage-format=histogram ./tools/scripts/sample.test.mts`

### Project Dependencies

The project has the following Maven dependencies defined:

- `org.graalvm.sdk:graal-sdk@24.2.0`

The project has the following NPM dependencies defined:

- `browserslist@4.24.4`
- `cssnano@7.0.6`
- `esbuild@0.25.2`
- `google-protobuf@3.21.4`
- `jszip@3.10.1`
- `lz-string@1.5.0`
- `postcss@8.5.3`
- `preact@10.26.5`
- `react@18.3.1`
- `react-dom@18.3.1`
- `typescript@5.8.3`

## Elide Runtime

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

