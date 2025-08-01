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
package dev.elide.secrets.dto.api.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API response for a GitHub repository information request.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
internal data class GithubRepositoryResponse(val private: Boolean, val permissions: Permissions) {
    @Serializable
    data class Permissions(
        @SerialName("push") val write: Boolean,
        @SerialName("pull") val read: Boolean,
    )
}
