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

import {
  initialize,
  context,
  formatMessages,
  formatMessagesSync,
  build,
  buildSync,
  version,
} from "mod.min.mjs"

export function setup() {
  console.log('Hello from esbuild setup');
}

export async function main() {
  const args = Array.from(arguments);
  console.log('Esbuild called with args', args);
  console.log('Esbuild mod', !!ESBUILD);
  console.log('Esbuild mod type', typeof ESBUILD);
  // let module;
  // try {
  //   console.log('Before instantiate');
  //   module = new WebAssembly.Module(ESBUILD);
  //   console.log('After instantiate');
  // } catch (err) {
  //   console.error('Failed to instantiate WebAssembly module', err);
  //   throw err;
  // }
  try {
    await initialize({
      wasmModule: ESBUILD,
    });
  } catch (err) {
    console.error("Failed to setup esbuild", err);
    throw err;
  }
  console.log('helllooo it worked');
}

export default main;

export {
  initialize,
  context,
  formatMessages,
  formatMessagesSync,
  build,
  buildSync,
  version,
}
