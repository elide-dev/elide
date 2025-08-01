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
package dev.elide.secrets.dto.persisted

import kotlinx.serialization.Serializable

/**
 * Metadata for secrets.
 *
 * @author Lauri Heino <datafox>
 */
@Serializable
public data class SecretMetadata(
    val name: String,
    val organization: String,
    val collections: Map<String, CollectionMetadata>,
) {
    init {
        collections.forEach {
            if (it.key != it.value.profile)
                throw IllegalStateException(
                    "Collection profile ${it.value.profile} does not match map key ${it.key}"
                )
        }
    }

    public fun add(collection: CollectionMetadata): SecretMetadata {
        return copy(collections = collections + (collection.profile to collection))
    }
}
