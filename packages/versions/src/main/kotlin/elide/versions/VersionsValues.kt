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
package elide.versions

/** @author Lauri Heino <datafox> */
public object VersionsValues {
  public const val CONFIG_FILE: String = "elide.json"
  public const val CATALOG_FILE: String = "catalog.json"
  public const val STAMP_FILE: String = "stampfile"
  public const val VERSION_FILE: String = "version"
  public const val PROJECT_VERSION_FILE: String = ".elideversion"

  public const val VERSIONS_COMMAND: String = "versions"
  public const val INSTALL_VERSION_FLAG: String = "--install-version"
  public const val UNINSTALL_VERSION_FLAG: String = "--uninstall-version"
  public const val INSTALL_PATH_FLAG: String = "--install-path"
  public const val NO_CONFIRM_FLAG: String = "--no-confirm"
  public const val ELEVATED_FLAG: String = "--elevated"
  public const val IGNORE_VERSION_FLAG: String = "--ignore-version"
  public const val USE_VERSION_FLAG: String = "--use-version"

  internal const val INSTALL_IO_BUFFER = 1024 * 1024L
  internal const val INSTALL_PROGRESS_INTERVAL = 20
}
