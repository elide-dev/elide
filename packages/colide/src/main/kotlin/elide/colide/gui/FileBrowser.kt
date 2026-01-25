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

package elide.colide.gui

import elide.colide.Vesa
import elide.colide.fs.FileSystem

/**
 * # File Browser Widget
 *
 * A file browser for navigating the /zip/ embedded filesystem.
 * Supports directory navigation, file selection, and basic operations.
 *
 * ## Features
 * - Directory tree navigation
 * - File type icons (folder, text, binary, executable)
 * - Double-click to open
 * - Selection callbacks
 */
public class FileBrowser : Widget() {
    
    private var currentPath = "/zip"
    private val entries = mutableListOf<FileEntry>()
    private var selectedIndex = -1
    private var scrollOffset = 0
    
    public var onFileSelected: ((FileEntry) -> Unit)? = null
    public var onFileOpened: ((FileEntry) -> Unit)? = null
    
    private val visibleRows: Int get() = (height - HEADER_HEIGHT - PADDING * 2) / ROW_HEIGHT
    
    /**
     * File entry in the browser.
     */
    public data class FileEntry(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long = 0,
        val extension: String = ""
    ) {
        val icon: String get() = when {
            isDirectory -> "[D]"
            extension in listOf("kt", "java", "js", "py", "c", "rs") -> "[S]"
            extension in listOf("com", "exe", "bin") -> "[X]"
            extension in listOf("txt", "md", "log") -> "[T]"
            extension in listOf("gguf", "bin", "dat") -> "[B]"
            else -> "[F]"
        }
    }
    
    init {
        backgroundColor = BG_COLOR
        foregroundColor = FG_COLOR
        focusable = true
        width = 300
        height = 400
        loadDirectory(currentPath)
    }
    
    override fun render() {
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        Vesa.fillRect(x, y, width, HEADER_HEIGHT, HEADER_BG)
        Font.drawText(x + PADDING, y + PADDING, currentPath, HEADER_FG)
        
        val contentY = y + HEADER_HEIGHT + PADDING
        val visibleEntries = entries.drop(scrollOffset).take(visibleRows)
        
        for ((idx, entry) in visibleEntries.withIndex()) {
            val rowY = contentY + idx * ROW_HEIGHT
            val actualIdx = scrollOffset + idx
            
            if (actualIdx == selectedIndex) {
                Vesa.fillRect(x + 2, rowY, width - 4, ROW_HEIGHT, SELECTED_BG)
            }
            
            val icon = if (entry.isDirectory) ">" else " "
            val displayName = if (entry.name.length > 30) {
                entry.name.take(27) + "..."
            } else {
                entry.name
            }
            
            val color = when {
                entry.isDirectory -> DIR_COLOR
                entry.extension in listOf("com", "exe") -> EXEC_COLOR
                else -> foregroundColor
            }
            
            Font.drawText(x + PADDING, rowY + 2, "$icon $displayName", color)
            
            if (!entry.isDirectory && entry.size > 0) {
                val sizeStr = formatSize(entry.size)
                val sizeX = x + width - PADDING - sizeStr.length * Font.CHAR_WIDTH
                Font.drawText(sizeX, rowY + 2, sizeStr, SIZE_COLOR)
            }
        }
        
        if (entries.size > visibleRows) {
            val scrollbarHeight = (visibleRows.toFloat() / entries.size * (height - HEADER_HEIGHT)).toInt()
            val scrollbarY = y + HEADER_HEIGHT + (scrollOffset.toFloat() / entries.size * (height - HEADER_HEIGHT)).toInt()
            Vesa.fillRect(x + width - 6, scrollbarY, 4, scrollbarHeight.coerceAtLeast(20), SCROLLBAR_COLOR)
        }
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        if (my < y + HEADER_HEIGHT) {
            navigateUp()
            return true
        }
        
        val contentY = y + HEADER_HEIGHT + PADDING
        val clickedRow = (my - contentY) / ROW_HEIGHT
        val clickedIndex = scrollOffset + clickedRow
        
        if (clickedIndex in entries.indices) {
            if (selectedIndex == clickedIndex) {
                openEntry(entries[clickedIndex])
            } else {
                selectedIndex = clickedIndex
                onFileSelected?.invoke(entries[clickedIndex])
            }
            return true
        }
        
        return false
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode) {
            0x101 -> { // Up
                if (selectedIndex > 0) {
                    selectedIndex--
                    ensureVisible(selectedIndex)
                    entries.getOrNull(selectedIndex)?.let { onFileSelected?.invoke(it) }
                }
                return true
            }
            0x102 -> { // Down
                if (selectedIndex < entries.lastIndex) {
                    selectedIndex++
                    ensureVisible(selectedIndex)
                    entries.getOrNull(selectedIndex)?.let { onFileSelected?.invoke(it) }
                }
                return true
            }
            '\n'.code, '\r'.code -> {
                entries.getOrNull(selectedIndex)?.let { openEntry(it) }
                return true
            }
            0x08 -> { // Backspace
                navigateUp()
                return true
            }
        }
        return false
    }
    
    /**
     * Navigate to a directory.
     */
    public fun navigateTo(path: String) {
        currentPath = path
        loadDirectory(path)
        selectedIndex = -1
        scrollOffset = 0
    }
    
    /**
     * Navigate up one directory.
     */
    public fun navigateUp() {
        if (currentPath != "/zip" && currentPath != "/") {
            val parent = currentPath.substringBeforeLast('/', "/zip")
            navigateTo(parent.ifEmpty { "/zip" })
        }
    }
    
    /**
     * Open selected entry.
     */
    private fun openEntry(entry: FileEntry) {
        if (entry.isDirectory) {
            navigateTo(entry.path)
        } else {
            onFileOpened?.invoke(entry)
        }
    }
    
    /**
     * Load directory contents from FileSystem.
     */
    private fun loadDirectory(path: String) {
        entries.clear()
        
        val fsEntries = FileSystem.listDirectory(path)
        for (fsEntry in fsEntries) {
            entries.add(FileEntry(
                name = fsEntry.name,
                path = fsEntry.path,
                isDirectory = fsEntry.isDirectory,
                size = fsEntry.size,
                extension = fsEntry.extension
            ))
        }
        
        if (entries.isEmpty() && path == "/zip") {
            entries.add(FileEntry("bin", "/zip/bin", true))
            entries.add(FileEntry("models", "/zip/models", true))
        }
    }
    
    /**
     * Ensure an index is visible.
     */
    private fun ensureVisible(index: Int) {
        if (index < scrollOffset) {
            scrollOffset = index
        } else if (index >= scrollOffset + visibleRows) {
            scrollOffset = index - visibleRows + 1
        }
    }
    
    /**
     * Format file size for display.
     */
    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000}G"
        bytes >= 1_000_000 -> "${bytes / 1_000_000}M"
        bytes >= 1_000 -> "${bytes / 1_000}K"
        else -> "${bytes}B"
    }
    
    /**
     * Get current path.
     */
    public fun getPath(): String = currentPath
    
    /**
     * Get selected entry.
     */
    public fun getSelected(): FileEntry? = entries.getOrNull(selectedIndex)
    
    public companion object {
        private const val PADDING = 4
        private const val ROW_HEIGHT = 20
        private const val HEADER_HEIGHT = 24
        private const val BG_COLOR = 0x00252535
        private const val FG_COLOR = 0x00eef5db
        private const val HEADER_BG = 0x00353545
        private const val HEADER_FG = 0x00ffffff
        private const val SELECTED_BG = 0x00404060
        private const val DIR_COLOR = 0x0066b3ff
        private const val EXEC_COLOR = 0x0027ae60
        private const val SIZE_COLOR = 0x00888888
        private const val SCROLLBAR_COLOR = 0x00555555
    }
}
