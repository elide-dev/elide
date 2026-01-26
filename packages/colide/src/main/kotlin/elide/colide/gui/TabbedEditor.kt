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
import java.util.UUID

/**
 * # Tabbed Editor
 *
 * A multi-document editor with tab bar.
 * Manages multiple CodeEditor instances with file tracking.
 */
public class TabbedEditor : Container() {
    
    private val tabBar = TabBar()
    private val editors = mutableMapOf<String, CodeEditor>()
    private var activeEditorId: String? = null
    
    public var onFileOpened: ((String) -> Unit)? = null
    public var onFileSaved: ((String) -> Unit)? = null
    public var onFileClosed: ((String) -> Unit)? = null
    public var onUnsavedClose: ((String, () -> Unit, () -> Unit) -> Unit)? = null
    
    init {
        tabBar.onTabSelected = { tab ->
            switchToEditor(tab.id)
        }
        tabBar.onTabClosed = { tab ->
            closeTab(tab.id)
        }
    }
    
    override fun render() {
        // Update tab bar position
        tabBar.x = x
        tabBar.y = y
        tabBar.width = width
        tabBar.render()
        
        // Render active editor
        activeEditorId?.let { id ->
            editors[id]?.let { editor ->
                editor.x = x
                editor.y = y + TabBar.TAB_HEIGHT
                editor.width = width
                editor.height = height - TabBar.TAB_HEIGHT
                editor.render()
            }
        }
        
        // Empty state
        if (editors.isEmpty()) {
            val msg = "No files open"
            val msgX = x + (width - msg.length * Font.CHAR_WIDTH) / 2
            val msgY = y + height / 2
            Font.drawText(msgX, msgY, msg, EMPTY_STATE_COLOR)
        }
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        activeEditorId?.let { id ->
            return editors[id]?.onKeyPress(keyCode) ?: false
        }
        return false
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        // Check tab bar
        if (my >= y && my < y + TabBar.TAB_HEIGHT) {
            return tabBar.onMouseClick(mx, my, button)
        }
        
        // Check active editor
        activeEditorId?.let { id ->
            return editors[id]?.onMouseClick(mx, my, button) ?: false
        }
        return false
    }
    
    override fun onMouseMove(mx: Int, my: Int) {
        tabBar.onMouseMove(mx, my)
        activeEditorId?.let { id ->
            editors[id]?.onMouseMove(mx, my)
        }
    }
    
    /**
     * Open a file in a new or existing tab.
     */
    public fun openFile(path: String): Boolean {
        // Check if already open
        val existingId = editors.entries.find { it.value.filePath == path }?.key
        if (existingId != null) {
            switchToEditor(existingId)
            return true
        }
        
        // Create new editor
        val editor = CodeEditor()
        if (!editor.loadFile(path)) {
            return false
        }
        
        val id = UUID.randomUUID().toString()
        val filename = path.substringAfterLast('/')
        
        editors[id] = editor
        tabBar.addTab(TabBar.Tab(
            id = id,
            title = filename,
            path = path,
            modified = false
        ))
        
        switchToEditor(id)
        onFileOpened?.invoke(path)
        return true
    }
    
    /**
     * Create a new untitled file.
     */
    public fun newFile(filename: String = "untitled.txt"): String {
        val id = UUID.randomUUID().toString()
        val editor = CodeEditor()
        editor.filePath = filename
        
        editors[id] = editor
        tabBar.addTab(TabBar.Tab(
            id = id,
            title = filename,
            path = null,
            modified = true
        ))
        
        switchToEditor(id)
        return id
    }
    
    /**
     * Save the active file.
     */
    public fun saveActiveFile(): Boolean {
        val id = activeEditorId ?: return false
        val editor = editors[id] ?: return false
        val path = editor.filePath ?: return false
        
        if (editor.saveFile(path)) {
            tabBar.setTabModified(id, false)
            onFileSaved?.invoke(path)
            return true
        }
        return false
    }
    
    /**
     * Save active file with new path.
     */
    public fun saveActiveFileAs(path: String): Boolean {
        val id = activeEditorId ?: return false
        val editor = editors[id] ?: return false
        
        if (editor.saveFile(path)) {
            editor.filePath = path
            val filename = path.substringAfterLast('/')
            tabBar.setTabTitle(id, filename)
            tabBar.setTabModified(id, false)
            onFileSaved?.invoke(path)
            return true
        }
        return false
    }
    
    /**
     * Close a tab by ID.
     */
    public fun closeTab(id: String) {
        val editor = editors[id] ?: return
        val tab = tabBar.getTab(id) ?: return
        
        if (tab.modified) {
            onUnsavedClose?.invoke(
                tab.path ?: tab.title,
                { // Save and close
                    if (saveFile(id)) {
                        doCloseTab(id)
                    }
                },
                { // Discard and close
                    doCloseTab(id)
                }
            )
        } else {
            doCloseTab(id)
        }
    }
    
    private fun doCloseTab(id: String) {
        val path = editors[id]?.filePath
        editors.remove(id)
        tabBar.removeTab(id)
        
        // Switch to another tab if needed
        if (activeEditorId == id) {
            activeEditorId = null
            tabBar.getActiveTab()?.let { tab ->
                switchToEditor(tab.id)
            }
        }
        
        path?.let { onFileClosed?.invoke(it) }
    }
    
    /**
     * Save a specific file by ID.
     */
    private fun saveFile(id: String): Boolean {
        val editor = editors[id] ?: return false
        val path = editor.filePath ?: return false
        
        if (editor.saveFile(path)) {
            tabBar.setTabModified(id, false)
            return true
        }
        return false
    }
    
    /**
     * Switch to a specific editor.
     */
    private fun switchToEditor(id: String) {
        if (!editors.containsKey(id)) return
        activeEditorId = id
        tabBar.selectTab(id)
    }
    
    /**
     * Get the active editor.
     */
    public fun getActiveEditor(): CodeEditor? = activeEditorId?.let { editors[it] }
    
    /**
     * Get the active file path.
     */
    public fun getActiveFilePath(): String? = activeEditorId?.let { editors[it]?.filePath }
    
    /**
     * Check if any file has unsaved changes.
     */
    public fun hasUnsavedChanges(): Boolean = tabBar.hasModifiedTabs()
    
    /**
     * Get count of open tabs.
     */
    public fun getTabCount(): Int = tabBar.getTabCount()
    
    /**
     * Mark active file as modified.
     */
    public fun markActiveModified(modified: Boolean = true) {
        activeEditorId?.let { id ->
            tabBar.setTabModified(id, modified)
        }
    }
    
    /**
     * Close all tabs, prompting for unsaved changes.
     */
    public fun closeAllTabs(onComplete: () -> Unit) {
        val modifiedTabs = tabBar.getModifiedTabs()
        if (modifiedTabs.isEmpty()) {
            editors.clear()
            while (tabBar.getTabCount() > 0) {
                tabBar.removeTab(0)
            }
            activeEditorId = null
            onComplete()
            return
        }
        
        // Handle first modified tab, recursively close others
        val firstModified = modifiedTabs.first()
        onUnsavedClose?.invoke(
            firstModified.path ?: firstModified.title,
            { // Save and continue
                saveFile(firstModified.id)
                doCloseTab(firstModified.id)
                closeAllTabs(onComplete)
            },
            { // Discard and continue
                doCloseTab(firstModified.id)
                closeAllTabs(onComplete)
            }
        )
    }
    
    public companion object {
        private const val EMPTY_STATE_COLOR = 0x006c7086
    }
}
