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
package elide.secrets

/**
 * Internal constants for secrets.
 *
 * @author Lauri Heino <datafox>
 */
internal object Values {
  // Cryptographic constants
  const val KEY_SIZE = 32
  const val IV_SIZE = 16
  const val HASH_ITERATIONS = 4096

  // File names
  const val DEFAULT_PATH = ".elide-secrets"
  const val METADATA_FILE = "metadata.json"
  const val LOCAL_FILE = "local.db"
  const val PROFILE_FILE_PREFIX = "secrets-"
  const val PROFILE_FILE_EXTENSION = ".db"
  const val KEY_FILE_EXTENSION = ".key"
  const val ACCESS_FILE_EXTENSION = ".access"
  const val SUPER_ACCESS_FILE = ".access"
  const val LOCAL_REMOTE_DEFAULT_PATH = ".secrets"

  // Local collection secrets
  const val REMOTE_SECRET = "remote"
  const val SELECTED_REMOTE_ACCESS_SECRET = "remote:access"
  const val SUPER_ACCESS_KEY_SECRET = "remote:super"
  const val REMOTE_ACCESS_KEY_SECRET = "remote:access:key"
  const val PROJECT_REMOTE_PATH_SECRET = "project:path"
  const val GITHUB_REPOSITORY_SECRET = "github:repository"
  const val GITHUB_TOKEN_SECRET = "github:token"

  // Environment variable names
  const val PASSPHRASE_ENVIRONMENT_VARIABLE = "ELIDE_SECRETS_PASSPHRASE"

  // Miscellaneous
  const val INVALID_PASSPHRASE_TRIES = 3
  const val SUPER_ACCESS_METADATA_NAME = "superAccess"
  const val GITHUB_API_URL = "https://api.github.com/"
  const val CLIENT_TIMEOUT = 10000L

  // Displayed text
  const val WELCOME_MESSAGE = "Welcome to Elide Secrets, let's get you set up!"
  const val INIT_OR_PULL_MESSAGE =
    "Do you want to initialize a new project or pull an existing project?\n" +
      "Please note that if you initialize a new project, you can only push it as a superuser."
  const val SELECT_ACCESS_IMPORT_MESSAGE =
    "Please select the access file you want to import.\n" +
      "Please note that for GPG-encrypted access files, only the ones that you have " +
            "a private key for on this system will be displayed."
  const val PASSPHRASES_NOT_IDENTICAL_MESSAGE = "Passphrases were not identical."
  const val INVALID_PASSPHRASE_MESSAGE = "Passphrases were not identical."
  const val SUPER_ACCESS_ENCRYPTION_MESSAGE =
    "How do you want to encrypt the remote super access file?\n" +
      "Please note that this file will be able to decrypt all secrets!"
  const val GITHUB_REMOTE_REPOSITORY_MESSAGE =
    "Elide Secrets on GitHub are stored as encrypted files committed to a private repository."
  const val GITHUB_REMOTE_TOKEN_MESSAGE =
    "To access GitHub, you need a personal access token.\n" +
      "If you do not have one with the required permissions, please head over to\n" +
      "https://github.com/settings/personal-access-tokens and generate a new token.\n" +
      "The token must at least have read access to \"Contents\" of the repository.\n" +
      "Optional write access to \"Contents\" allows you to update the remote secrets."

  const val GENERIC_CHOICE_PROMPT = "Please select an option:"
  const val PUSH_PROFILES_PROMPT = "Please select the changed profiles you want to push:"
  const val SUPERUSER_PASSPHRASE_PROMPT = "Please enter the superuser passphrase:"
  const val PROJECT_NAME_PROMPT = "What is the name of your project?"
  const val ORGANIZATION_NAME_PROMPT = "What is the name of your organization?"
  const val PULL_AS_SUPERUSER_PROMPT = "Do you want to pull as a superuser?"
  const val ACCESS_PASSPHRASE_PROMPT = "Please enter the passphrase for this access file:"
  const val REMOTE_SECRETS_LOCATION_PROMPT = "Please select where to access remote secrets:"
  const val ENTER_PASSPHRASE_PROMPT = "Please enter your passphrase:"
  const val ENTER_PASSPHRASE_REPEAT_PROMPT = "Please enter your passphrase again:"
  const val LOCAL_STORAGE_ENCRYPTION_PROMPT = "How do you want to encrypt locally stored secrets?"
  const val ACCESS_FILE_ENCRYPTION_PROMPT = "How do you want to encrypt this access file?"
  const val GPG_PRIVATE_KEY_PROMPT = "Please select a private key"
  const val GPG_PUBLIC_KEY_PROMPT = "Please select a public key"
  const val GITHUB_REMOTE_REPOSITORY_PROMPT = "Please enter the repository identity (\"owner/repository\"):"
  const val GITHUB_REMOTE_TOKEN_PROMPT = "Please enter your personal access token:"

  const val INITIALIZE_PROJECT_OPTION = "Initialize a new project"
  const val PULL_PROJECT_OPTION = "Pull an existing project"

  const val SECRETS_NOT_INITIALIZED_WARNING = "Secrets are not initialized, please run \"elide secrets\" first."
  const val SECRETS_STATE_INITIALIZED_WARNING = "SecretsState init already called"

  const val PASSPHRASE_READ_EXCEPTION = "Could not read the passphrase from environment."
  const val REMOVED_PROFILE_NOT_SELECTED_EXCEPTION = "Can only remove the currently selected profile."
  const val PROFILE_LOADED_PUSH_EXCEPTION = "Please deselect the selected profile before pushing."
  const val PROFILE_LOADED_PULL_EXCEPTION = "Please deselect the selected profile before pulling."
  const val REMOTE_MANAGEMENT_ONLY_EXCEPTION =
    "Secrets were created locally or pulled as a superuser, please use remote management instead."
  const val REMOTE_NOT_INITIALIZED_EXCEPTION = "Remote is not initialized, please use remote management instead."
  const val MISMATCHING_PASSPHRASES_EXCEPTION = "Mismatching passphrases entered too many times."
  const val INVALID_PASSPHRASE_EXCEPTION = "Invalid passphrase entered too many times."
  const val NOT_IN_INTERACTIVE_MODE_EXCEPTION = "Application is not in interactive mode."
  const val GITHUB_REMOTE_REPOSITORY_NOT_PRIVATE_EXCEPTION = "The repository is not private."
  const val GITHUB_REMOTE_REPOSITORY_NO_READ_ACCESS_EXCEPTION = "No read access to repository contents."

  fun profileDoesNotExistException(profile: String) = "Profile \"$profile\" does not exist."

  fun profileAlreadyExistsException(profile: String) = "Profile \"$profile\" already exists."
}
