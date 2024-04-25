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

import esbuild from "esbuild";

export const mode = 'esm'

async function buildTypescriptEntrypoint() {
  await esbuild.build({
    entryPoints: ["src/main/js/entry.mjs"],
    outfile: `src/main/resources/META-INF/elide/embedded/tools/tsc/entry.${mode === 'esm' ? 'mjs' : 'cjs'}`,
    format: mode,
    bundle: true,
    platform: "neutral",
    external: ["fs", "node:fs", "path", "node:path", "os", "node:os", "inspector", "node:inspector"],
    mainFields: ["module", "main"]
  })

  console.info("TypeScript entrypoint build complete.")
}

console.info("Building TypeScript entrypoint...");
await buildTypescriptEntrypoint();
