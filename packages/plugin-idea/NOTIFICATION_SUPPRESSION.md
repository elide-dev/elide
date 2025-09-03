# Pkl Notification Suppression for Elide Projects

## Problem

When using IntelliJ IDEA with both the Elide plugin and the Pkl plugin installed, editing `elide.pkl` files (Elide project manifests) would trigger unwanted sync banners from the Pkl plugin. This happened because:

1. The Pkl plugin detects any `.pkl` file and assumes it might be a standalone Pkl project
2. It shows sync banners asking users if they want to sync the "Pkl project"
3. However, `elide.pkl` files are Elide project manifests, not standalone Pkl projects

## Solution

We implemented a multi-layered approach to suppress these unwanted notifications:

### 1. ElideNotificationSuppressor

**File**: `src/main/kotlin/dev/elide/intellij/project/ElideNotificationSuppressor.kt`

This class implements `UnlinkedProjectNotificationAware` to:
- Detect when the Pkl plugin is about to show a sync notification
- Check if the file is an Elide manifest or is part of an Elide project
- Suppress the notification if it relates to Elide files
- Allow normal Pkl notifications for legitimate standalone Pkl projects

**Key Features**:
- Detects `elide.pkl` files directly
- Detects `.pkl` files in directories containing `elide.pkl`
- Checks up to 3 parent directories for Elide manifests
- Returns empty notification actions for Elide-related files

### 2. ElideNotificationManager

**File**: `src/main/kotlin/dev/elide/intellij/project/ElideNotificationManager.kt`

This service provides:
- Project-level notification management
- Content-based notification filtering
- Global notification interception
- Comprehensive logging for debugging

**Key Features**:
- Analyzes notification content for Pkl/sync keywords
- Detects mentions of `elide.pkl` in notification text
- Expires unwanted notifications before they reach users
- Maintains project-specific context

### 3. ElideNotificationStartupActivity

**File**: `src/main/kotlin/dev/elide/intellij/project/ElideNotificationManager.kt`

This startup activity:
- Registers global notification listeners on project startup
- Automatically configures suppression for new projects
- Ensures notification filtering is active throughout the project lifecycle

## Configuration

The suppression is automatically registered in `plugin-pkl.xml`:

```xml
<!-- Suppress Pkl project sync notifications for Elide manifest files -->
<externalSystem.unlinkedProjectNotificationAware 
                implementation="dev.elide.intellij.project.ElideNotificationSuppressor"/>
<postStartupActivity implementation="dev.elide.intellij.project.ElideNotificationStartupActivity"/>
```

## Testing

Tests are provided in `ElideNotificationSuppressorTest.kt` to verify:
- ✅ Notifications are suppressed for `elide.pkl` files
- ✅ Notifications are suppressed for `.pkl` files in Elide projects
- ✅ Notifications are allowed for standalone `.pkl` files
- ✅ Empty actions are returned for Elide-related files

## Benefits

1. **Better User Experience**: No more errant sync banners when editing Elide manifests
2. **Project Context Awareness**: The plugin understands the difference between Elide manifests and standalone Pkl files
3. **Non-Invasive**: Doesn't interfere with legitimate Pkl project functionality
4. **Robust Detection**: Uses multiple detection strategies to catch various scenarios
5. **Backwards Compatible**: Works with existing Elide projects without requiring changes

## Edge Cases Handled

- Files in subdirectories of Elide projects
- Projects with nested directory structures
- Mixed projects with both Elide manifests and standalone Pkl files
- Project opening and closing scenarios
- IDE restart scenarios

## Future Considerations

- Monitor for changes in the Pkl plugin API that might affect notification behavior
- Consider extending suppression to other file types if needed
- Add user configuration options if requested
- Monitor performance impact of notification filtering

## Related Issues

- **GitHub Issue**: [#1586 - IDEA: Errant Pkl sync banner](https://github.com/elide-dev/elide/issues/1586)
- **Root Cause**: Pkl plugin `org.pkl` detects `elide.pkl` files as potential Pkl projects
- **Resolution**: Multi-layered notification suppression system