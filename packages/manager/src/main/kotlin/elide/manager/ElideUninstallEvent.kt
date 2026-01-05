package elide.manager

/**
 * Events for [InstallManager.uninstall].
 *
 * @author Lauri Heino <datafox>
 */
public sealed interface ElideUninstallEvent

public data object UninstallStartEvent : ElideUninstallEvent

public data class UninstallProgressEvent(val progress: Float, val name: String) : ElideUninstallEvent

public data object UninstallCompletedEvent : ElideUninstallEvent
