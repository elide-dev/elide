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

import dev.elide.secrets.dto.persisted.Secret

/**
 * Main entrypoint for secrets.
 *
 * @author Lauri Heino <datafox>
 */
public interface Secrets {
    /** Initializes the secret system. */
    public suspend fun init()

    /** Creates a new profile. */
    public suspend fun createProfile(profile: String)

    /** Removes a profile. */
    public suspend fun removeProfile(profile: String)

    /** Returns names of all profiles. */
    public suspend fun getProfiles(): List<String>

    /**
     * Returns names of all profiles on a remote, connecting to a remote if a connection has not
     * been made.
     */
    public suspend fun getRemoteProfiles(): List<String>

    /**
     * Updates specified [profiles] on the local by downloading them from a remote, connecting to a
     * remote if a connection has not been made. If [profiles] is empty, downloads all profiles on
     * the remote.
     */
    public suspend fun updateLocal(vararg profiles: String)

    /**
     * Uploads specified [profiles] to a remote, connecting to a remote if a connection has not been
     * made. If [profiles] is empty, uploads all profiles to the remote.
     */
    public suspend fun updateRemote(vararg profiles: String)

    /** Selects a profile for access to secrets. */
    public suspend fun selectProfile(profile: String)

    /**
     * Returns a secret value with [name] from the selected profile, or throws an
     * [IllegalStateException] if a profile has not been selected.
     */
    public suspend fun getSecret(name: String): Any?

    /** Returns a secret value with [name] from [profile]. */
    public suspend fun getSecret(profile: String, name: String): Any?

    /**
     * Sets a secret to the selected profile, overwriting any existing secret with the same name, or
     * throws an [IllegalStateException] if a profile has not been selected.
     */
    public suspend fun setSecret(secret: Secret<*>)

    /**
     * Removes a secret value with [name] from the selected profile, or throws an
     * [IllegalStateException] if a profile has not been selected.
     */
    public suspend fun removeSecret(name: String)

    /**
     * Writes changes to the selected profile, or throws an [IllegalStateException] if a profile has
     * not been selected.
     */
    public suspend fun writeChanges()

    /**
     * Deselects the currently selected profile. Does nothing if a profile has not been selected.
     */
    public suspend fun deselectProfile()

    /** Removes the currently selected profile. */
    public suspend fun removeProfile()

    /**
     * Removes the specified profile from a remote, connecting to a remote if a connection has not
     * been made.
     */
    public suspend fun removeRemoteProfile(profile: String)

    public companion object {
        /** Returns the return value of [getSecret] cast to the reified type [T]. */
        public suspend inline fun <reified T> Secrets.getTypedSecret(name: String): T? =
            getSecret(name) as T?

        /** Returns the return value of [getSecret] cast to the reified type [T]. */
        public suspend inline fun <reified T> Secrets.getTypedSecret(
            profile: String,
            name: String,
        ): T? = getSecret(profile, name) as T?
    }
}
