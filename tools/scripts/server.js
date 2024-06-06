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

const { ok } = require("node:assert");
ok(typeof Elide.http !== "undefined", "please run with `elide serve`");

// access the built-in HTTP server engine
const app = Elide.http;

// register basic handler
app.router.handle("GET", "/plaintext", (request, response) => {
  // respond using the captured path variables
  response.send(200, `Hello, World!`);
});

// register json handler
app.router.handle("GET", "/json", (request, response) => {
  // respond using the captured path variables
  response.send(200, JSON.stringify({ message: "Hello, World!" }));
});


// register a route handler
app.router.handle("GET", "/hello/:name", (request, response, context) => {
  // respond using the captured path variables
  response.send(200, `Hello, ${context.params.name}`);
});

// configure the server binding options
app.config.port = 3000;

// receive a callback when the server starts
app.config.onBind(() => {
  console.log(`Server listening at "http://localhost:${app.config.port}"! 🚀`);
});

// start the server
app.start();
