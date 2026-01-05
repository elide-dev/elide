package elide.manager

/**
 * Events for [InstallManager.install].
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
 * Events for [InstallManager.verifyInstall].
 *
 * @author Lauri Heino <datafox>
 */
public sealed interface ElideFileVerifyEvent : ElideInstallEvent

public data object FileVerifyStartEvent : ElideFileVerifyEvent

public data class FileVerifyProgressEvent(val progress: Float, val name: String) : ElideFileVerifyEvent

public data object FileVerifyCompletedEvent : ElideFileVerifyEvent

public data object FileVerifyIndeterminateEvent : ElideFileVerifyEvent
