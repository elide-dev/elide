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

/**
 * Internal constants for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal object Values {
  const val KEY_SIZE = 32
  const val IV_SIZE = 16
  const val HASH_ITERATIONS = 4096
  const val DEFAULT_PATH = ".elide-secrets"
  const val METADATA_FILE = "metadata.json"
  const val LOCAL_FILE = "local.db"
  const val PROFILE_FILE_PREFIX = "secrets-"
  const val PROFILE_FILE_EXTENSION = ".db"
  const val KEY_FILE_EXTENSION = ".key"
  const val ACCESS_FILE_EXTENSION = ".access"
  const val SUPER_ACCESS_FILE = ".access"
  const val LOCAL_REMOTE_DEFAULT_PATH = ".secrets"
  const val PASSPHRASE_ENVIRONMENT_VARIABLE = "ELIDE_SECRETS_PASSPHRASE"
  const val INVALID_PASSPHRASE_TRIES = 3
}
