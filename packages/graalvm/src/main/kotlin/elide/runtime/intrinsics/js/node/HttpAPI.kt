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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.runtime.intrinsics.js.node.http.HttpServerAPI
import elide.vm.annotations.Polyglot

public typealias ServerRequestListener = Value
public typealias CreateServerOptions = Value

/**
 * ## Node API: HTTP
 */
@API public interface HttpAPI : NodeAPI {
  @Polyglot public fun createServer(
    options: CreateServerOptions? = null,
    listener: ServerRequestListener? = null
  ): HttpServerAPI
}

