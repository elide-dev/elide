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

package elide.embedded.http

import elide.vm.annotations.Polyglot

/**
 * Represents an HTTP request passed to the embedded runtime by the host application.
 *
 * Requests may be passed using a serialization protocol such as ProtoBuf or Cap'n'Proto, or directly using structs
 * over a native boundary. The exchange mechanism may influence the behavior of certain methods and properties, but
 * the general contract is kept regardless of the provenance.
 */
public interface EmbeddedRequest
