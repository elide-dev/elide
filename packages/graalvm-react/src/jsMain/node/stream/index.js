/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

const {
  Readable,
  Writable,
  Transform,
  Duplex,
  pipeline,
  finished,
} = require("readable-stream");

module.exports.Readable = Readable;
module.exports.Writable = Writable;
module.exports.Transform = Transform;
module.exports.Duplex = Duplex;
module.exports.pipeline = pipeline;
module.exports.finished = finished;
