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

/**
 * Events for [VersionManager.install].
 *
 * @author Lauri Heino <datafox>
 */
public sealed interface ElideInstallEvent

public data object DownloadStartEvent : ElideInstallEvent

public data class DownloadProgressEvent(val progress: Float) : ElideInstallEvent

public data object DownloadCompletedEvent : ElideInstallEvent

public data object VerifyStartEvent : ElideInstallEvent

public data class VerifyProgressEvent(val progress: Float) : ElideInstallEvent

public data object VerifyCompletedEvent : ElideInstallEvent

public data object InstallStartEvent : ElideInstallEvent

public data class InstallProgressEvent(val progress: Float) : ElideInstallEvent

public data class InstallFileEvent(val name: String) : ElideInstallEvent

public data object InstallCompletedEvent : ElideInstallEvent

/**
 * Events for [VersionManager.verifyInstall].
 *
 * @author Lauri Heino <datafox>
 */
public sealed interface ElideFileVerifyEvent : ElideInstallEvent

public data object FileVerifyStartEvent : ElideFileVerifyEvent

public data class FileVerifyProgressEvent(val progress: Float, val name: String) : ElideFileVerifyEvent

public data object FileVerifyCompletedEvent : ElideFileVerifyEvent

public data object FileVerifyIndeterminateEvent : ElideFileVerifyEvent
