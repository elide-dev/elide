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
 * # Project Tree
 *
 * A project-aware file browser with:
 * - Project root detection (.git, package.json, etc.)
 * - Gitignore filtering
 * - File type icons
 * - Expand/collapse state memory
 */
public class ProjectTree : Widget() {
    
    public data class TreeNode(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val depth: Int,
        var expanded: Boolean = false,
        var children: MutableList<TreeNode> = mutableListOf()
    )
    
    private var rootNode: TreeNode? = null
    private var flatNodes = mutableListOf<TreeNode>()
    private var selectedIndex: Int = -1
    private var scrollOffset: Int = 0
    private var hoverIndex: Int = -1
    
    private val expandedPaths = mutableSetOf<String>()
    private val gitignorePatterns = mutableListOf<String>()
    
    public var projectRoot: String? = null
    public var onFileSelected: ((String) -> Unit)? = null
    public var onDirectorySelected: ((String) -> Unit)? = null
    
    init {
        backgroundColor = BG_COLOR
        foregroundColor = FG_COLOR
    }
    
    override fun render() {
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        if (rootNode == null) {
            val msg = "No project open"
            Font.drawText(x + PADDING, y + PADDING, msg, DIMMED_COLOR)
            return
        }
        
        val visibleItems = height / ITEM_HEIGHT
        var itemY = y
        
        for (i in scrollOffset until minOf(scrollOffset + visibleItems, flatNodes.size)) {
            val node = flatNodes[i]
            val isSelected = i == selectedIndex
            val isHover = i == hoverIndex
            
            renderNode(itemY, node, isSelected, isHover)
            itemY += ITEM_HEIGHT
        }
        
        // Scrollbar
        if (flatNodes.size > visibleItems) {
            renderScrollbar(visibleItems)
        }
    }
    
    private fun renderNode(nodeY: Int, node: TreeNode, isSelected: Boolean, isHover: Boolean) {
        val indent = PADDING + node.depth * INDENT_SIZE
        
        // Selection/hover background
        if (isSelected) {
            Vesa.fillRect(x, nodeY, width, ITEM_HEIGHT, SELECTED_BG)
        } else if (isHover) {
            Vesa.fillRect(x, nodeY, width, ITEM_HEIGHT, HOVER_BG)
        }
        
        var drawX = x + indent
        
        // Expand/collapse indicator for directories
        if (node.isDirectory) {
            val indicator = if (node.expanded) "v" else ">"
            Font.drawText(drawX, nodeY + 4, indicator, ARROW_COLOR)
        }
        drawX += Font.CHAR_WIDTH + 4
        
        // File type icon
        val icon = getFileIcon(node)
        val iconColor = getIconColor(node)
        Font.drawText(drawX, nodeY + 4, icon, iconColor)
        drawX += Font.CHAR_WIDTH * 2 + 4
        
        // Name
        val fg = when {
            isSelected -> SELECTED_FG
            node.isDirectory -> DIR_COLOR
            else -> foregroundColor
        }
        Font.drawText(drawX, nodeY + 4, node.name, fg)
    }
    
    private fun renderScrollbar(visibleItems: Int) {
        val scrollbarX = x + width - SCROLLBAR_WIDTH
        val scrollbarHeight = height
        val thumbHeight = maxOf(20, (visibleItems * height) / flatNodes.size)
        val thumbY = y + (scrollOffset * (scrollbarHeight - thumbHeight)) / (flatNodes.size - visibleItems)
        
        Vesa.fillRect(scrollbarX, y, SCROLLBAR_WIDTH, scrollbarHeight, SCROLLBAR_BG)
        Vesa.fillRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_THUMB)
    }
    
    private fun getFileIcon(node: TreeNode): String {
        if (node.isDirectory) {
            return if (node.expanded) "[" else "+"
        }
        
        val ext = node.name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts" -> "K"
            "java" -> "J"
            "js", "mjs" -> "j"
            "ts", "mts" -> "T"
            "py" -> "P"
            "rs" -> "R"
            "c", "h" -> "C"
            "cpp", "hpp" -> "#"
            "md" -> "M"
            "json" -> "{"
            "yaml", "yml" -> "Y"
            "toml" -> "="
            "xml" -> "<"
            "html", "htm" -> "H"
            "css" -> "*"
            "sh", "bash" -> "$"
            "sql" -> "S"
            "txt" -> "t"
            "gitignore" -> "G"
            else -> "."
        }
    }
    
    private fun getIconColor(node: TreeNode): Int {
        if (node.isDirectory) {
            return FOLDER_COLOR
        }
        
        val ext = node.name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts" -> KOTLIN_COLOR
            "java" -> JAVA_COLOR
            "js", "mjs", "ts", "mts" -> JS_COLOR
            "py" -> PYTHON_COLOR
            "rs" -> RUST_COLOR
            "c", "h", "cpp", "hpp" -> C_COLOR
            "md" -> MD_COLOR
            "json", "yaml", "yml", "toml" -> CONFIG_COLOR
            "xml", "html", "htm" -> HTML_COLOR
            else -> foregroundColor
        }
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        if (mx < x || mx >= x + width || my < y || my >= y + height) return false
        
        val index = scrollOffset + (my - y) / ITEM_HEIGHT
        if (index < 0 || index >= flatNodes.size) return false
        
        val node = flatNodes[index]
        selectedIndex = index
        
        if (node.isDirectory) {
            if (button == 1) {
                toggleExpand(node)
            }
            onDirectorySelected?.invoke(node.path)
        } else {
            if (button == 1) {
                onFileSelected?.invoke(node.path)
            }
        }
        
        return true
    }
    
    override fun onMouseMove(mx: Int, my: Int) {
        hoverIndex = -1
        if (mx < x || mx >= x + width || my < y || my >= y + height) return
        
        val index = scrollOffset + (my - y) / ITEM_HEIGHT
        if (index >= 0 && index < flatNodes.size) {
            hoverIndex = index
        }
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode) {
            0x26 -> { // Up
                if (selectedIndex > 0) {
                    selectedIndex--
                    ensureVisible(selectedIndex)
                }
                return true
            }
            0x28 -> { // Down
                if (selectedIndex < flatNodes.size - 1) {
                    selectedIndex++
                    ensureVisible(selectedIndex)
                }
                return true
            }
            0x25 -> { // Left - collapse
                if (selectedIndex >= 0) {
                    val node = flatNodes[selectedIndex]
                    if (node.isDirectory && node.expanded) {
                        toggleExpand(node)
                    }
                }
                return true
            }
            0x27 -> { // Right - expand
                if (selectedIndex >= 0) {
                    val node = flatNodes[selectedIndex]
                    if (node.isDirectory && !node.expanded) {
                        toggleExpand(node)
                    }
                }
                return true
            }
            0x0D -> { // Enter
                if (selectedIndex >= 0) {
                    val node = flatNodes[selectedIndex]
                    if (node.isDirectory) {
                        toggleExpand(node)
                        onDirectorySelected?.invoke(node.path)
                    } else {
                        onFileSelected?.invoke(node.path)
                    }
                }
                return true
            }
        }
        return false
    }
    
    private fun ensureVisible(index: Int) {
        val visibleItems = height / ITEM_HEIGHT
        if (index < scrollOffset) {
            scrollOffset = index
        } else if (index >= scrollOffset + visibleItems) {
            scrollOffset = index - visibleItems + 1
        }
    }
    
    private fun toggleExpand(node: TreeNode) {
        node.expanded = !node.expanded
        if (node.expanded) {
            expandedPaths.add(node.path)
            loadChildren(node)
        } else {
            expandedPaths.remove(node.path)
        }
        rebuildFlatList()
    }
    
    /**
     * Open a project directory.
     */
    public fun openProject(path: String) {
        projectRoot = findProjectRoot(path) ?: path
        loadGitignore()
        
        rootNode = TreeNode(
            name = projectRoot!!.substringAfterLast('/'),
            path = projectRoot!!,
            isDirectory = true,
            depth = 0,
            expanded = true
        )
        
        loadChildren(rootNode!!)
        rebuildFlatList()
        selectedIndex = 0
    }
    
    /**
     * Find project root by looking for marker files.
     */
    private fun findProjectRoot(path: String): String? {
        var current = path
        val markers = listOf(".git", "package.json", "build.gradle", "build.gradle.kts", "Cargo.toml", "pom.xml")
        
        while (current.isNotEmpty() && current != "/") {
            for (marker in markers) {
                val markerPath = "$current/$marker"
                if (FileSystem.exists(markerPath)) {
                    return current
                }
            }
            current = current.substringBeforeLast('/')
        }
        return null
    }
    
    /**
     * Load .gitignore patterns if present.
     */
    private fun loadGitignore() {
        gitignorePatterns.clear()
        projectRoot?.let { root ->
            val gitignorePath = "$root/.gitignore"
            if (FileSystem.exists(gitignorePath)) {
                val content = FileSystem.readText(gitignorePath)
                content?.lines()?.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        gitignorePatterns.add(trimmed)
                    }
                }
            }
        }
        // Always ignore common patterns
        gitignorePatterns.addAll(listOf(
            ".git", "node_modules", "__pycache__", ".idea", ".vscode",
            "build", "dist", "target", ".gradle", "*.class", "*.pyc"
        ))
    }
    
    /**
     * Check if a file should be ignored.
     */
    private fun shouldIgnore(name: String, isDirectory: Boolean): Boolean {
        for (pattern in gitignorePatterns) {
            if (pattern.endsWith("/") && isDirectory) {
                if (name == pattern.dropLast(1)) return true
            } else if (pattern.startsWith("*.")) {
                val ext = pattern.drop(2)
                if (name.endsWith(".$ext")) return true
            } else if (name == pattern) {
                return true
            }
        }
        return false
    }
    
    /**
     * Load children for a directory node.
     */
    private fun loadChildren(node: TreeNode) {
        node.children.clear()
        
        val entries = FileSystem.listDirectory(node.path)
        if (entries.isEmpty()) return
        
        // Sort: directories first, then alphabetically
        val sorted = entries
            .filter { !shouldIgnore(it.name, it.isDirectory) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        
        for (entry in sorted) {
            val childNode = TreeNode(
                name = entry.name,
                path = entry.path,
                isDirectory = entry.isDirectory,
                depth = node.depth + 1,
                expanded = expandedPaths.contains(entry.path)
            )
            
            if (childNode.expanded && childNode.isDirectory) {
                loadChildren(childNode)
            }
            
            node.children.add(childNode)
        }
    }
    
    /**
     * Rebuild the flat list for rendering.
     */
    private fun rebuildFlatList() {
        flatNodes.clear()
        rootNode?.let { addNodeToFlat(it) }
    }
    
    private fun addNodeToFlat(node: TreeNode) {
        flatNodes.add(node)
        if (node.expanded) {
            for (child in node.children) {
                addNodeToFlat(child)
            }
        }
    }
    
    /**
     * Refresh the tree from disk.
     */
    public fun refresh() {
        rootNode?.let { root ->
            loadChildren(root)
            rebuildFlatList()
        }
    }
    
    /**
     * Get the selected file path.
     */
    public fun getSelectedPath(): String? {
        return if (selectedIndex >= 0 && selectedIndex < flatNodes.size) {
            flatNodes[selectedIndex].path
        } else null
    }
    
    public companion object {
        private const val ITEM_HEIGHT = 20
        private const val INDENT_SIZE = 16
        private const val PADDING = 8
        private const val SCROLLBAR_WIDTH = 8
        
        private const val BG_COLOR = 0x001e1e2e
        private const val FG_COLOR = 0x00cdd6f4
        private const val DIMMED_COLOR = 0x006c7086
        private const val SELECTED_BG = 0x00313244
        private const val SELECTED_FG = 0x00cdd6f4
        private const val HOVER_BG = 0x00252535
        private const val ARROW_COLOR = 0x006c7086
        private const val DIR_COLOR = 0x0089b4fa
        
        private const val SCROLLBAR_BG = 0x00181825
        private const val SCROLLBAR_THUMB = 0x00585b70
        
        // File type colors
        private const val FOLDER_COLOR = 0x00f9e2af
        private const val KOTLIN_COLOR = 0x00cba6f7
        private const val JAVA_COLOR = 0x00f38ba8
        private const val JS_COLOR = 0x00f9e2af
        private const val PYTHON_COLOR = 0x0094e2d5
        private const val RUST_COLOR = 0x00fab387
        private const val C_COLOR = 0x0089b4fa
        private const val MD_COLOR = 0x00a6e3a1
        private const val CONFIG_COLOR = 0x006c7086
        private const val HTML_COLOR = 0x00f38ba8
    }
}
