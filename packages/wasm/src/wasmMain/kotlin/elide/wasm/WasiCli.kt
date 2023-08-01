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

package elide.wasm

import org.kowasm.wasi.internal.argsGet
import org.kowasm.wasi.internal.environGet

/**
 * Provides access to the command line arguments and environment variables.
 */
interface WasiCli {

    /**
     * The command line arguments.
     */
    val args: List<String>

    /**
     * The environment variables.
     */
    val envVars: List<Pair<String, String>>
}

/**
 * Default implementation of [WasiCli].
 */
object DefaultWasiCli : WasiCli {

    override val args: List<String>
        get() = argsGet()

    override val envVars: List<Pair<String, String>>
        get() = environGet().map { Pair(it.key, it.value) }
}
