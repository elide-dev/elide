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
package dev.elide.secrets

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.io.files.Path

/**
 * State of secrets.
 *
 * @property interactive If `true`, reading input from the console is permitted.
 * @property path path containing secrets files.
 */
internal class SecretsState(val interactive: Boolean, val path: Path) {
    companion object {
        private val _instance: CompletableDeferred<SecretsState> = CompletableDeferred()
        val instance: Deferred<SecretsState> = _instance

        fun set(state: SecretsState) = _instance.complete(state)

        suspend fun get(): SecretsState = instance.await()
    }
}
