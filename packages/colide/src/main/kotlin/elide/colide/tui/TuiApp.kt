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

package elide.colide.tui

import elide.colide.ColideNative
import elide.colide.Keyboard

/**
 * # TUI Application
 *
 * Base class for TUI applications in Colide OS.
 * Manages rendering, input handling, and component lifecycle.
 *
 * ## Usage
 * ```kotlin
 * class MyApp : TuiApp("My App") {
 *     override fun onCreate() {
 *         val label = TuiLabel(1, 1, "Hello World!")
 *         addComponent(label)
 *     }
 * }
 * ```
 */
public abstract class TuiApp(
    public val title: String,
    width: Int = 80,
    height: Int = 25
) {
    protected val renderer: TuiRenderer = TuiRenderer(width, height)
    protected val components: MutableList<TuiComponent> = mutableListOf()
    protected var running: Boolean = false
    protected var focusedIndex: Int = -1
    
    protected var statusBar: TuiStatusBar? = null
    
    private val isMetal = ColideNative.isAvailable && ColideNative.isMetal()
    
    /**
     * Called when the app is created.
     */
    protected abstract fun onCreate()
    
    /**
     * Called each frame before rendering.
     */
    protected open fun onUpdate() {}
    
    /**
     * Called when a key is pressed and not handled by a component.
     */
    protected open fun onKeyPress(key: Int): Boolean = false
    
    /**
     * Called when the app is about to exit.
     */
    protected open fun onDestroy() {}
    
    /**
     * Start the application.
     */
    public fun run() {
        running = true
        onCreate()
        
        if (statusBar == null) {
            statusBar = TuiStatusBar(renderer.getHeight() - 1, renderer.getWidth())
            statusBar?.leftText = title
            statusBar?.rightText = "q=quit"
        }
        
        while (running) {
            onUpdate()
            render()
            processInput()
        }
        
        onDestroy()
    }
    
    /**
     * Stop the application.
     */
    public fun quit() {
        running = false
    }
    
    /**
     * Render all components.
     */
    protected fun render() {
        renderer.clear()
        
        for (component in components) {
            component.render(renderer)
        }
        
        statusBar?.render(renderer)
        renderer.render()
    }
    
    /**
     * Process keyboard input.
     */
    protected fun processInput() {
        val key = readKey()
        if (key == 0) return
        
        if (key == 'q'.code && !hasFocusedInput()) {
            quit()
            return
        }
        
        if (key == '\t'.code) {
            focusNext()
            return
        }
        
        if (focusedIndex in components.indices) {
            val focused = components[focusedIndex]
            if (focused.handleKey(key)) return
        }
        
        onKeyPress(key)
    }
    
    /**
     * Read a key from input.
     */
    private fun readKey(): Int {
        return if (isMetal) {
            if (Keyboard.available()) Keyboard.getChar() else 0
        } else {
            try {
                if (System.`in`.available() > 0) {
                    System.`in`.read()
                } else {
                    Thread.sleep(16)
                    0
                }
            } catch (_: Exception) {
                0
            }
        }
    }
    
    /**
     * Check if focused component is an input field.
     */
    private fun hasFocusedInput(): Boolean {
        if (focusedIndex !in components.indices) return false
        return components[focusedIndex] is TuiInput
    }
    
    /**
     * Add a component to the app.
     */
    protected fun addComponent(component: TuiComponent) {
        components.add(component)
    }
    
    /**
     * Remove a component from the app.
     */
    protected fun removeComponent(component: TuiComponent) {
        val index = components.indexOf(component)
        if (index >= 0) {
            components.removeAt(index)
            if (focusedIndex >= components.size) {
                focusedIndex = components.size - 1
            }
        }
    }
    
    /**
     * Focus the next focusable component.
     */
    protected fun focusNext() {
        if (components.isEmpty()) return
        
        if (focusedIndex in components.indices) {
            components[focusedIndex].focused = false
        }
        
        focusedIndex = (focusedIndex + 1) % components.size
        components[focusedIndex].focused = true
    }
    
    /**
     * Focus the previous focusable component.
     */
    protected fun focusPrev() {
        if (components.isEmpty()) return
        
        if (focusedIndex in components.indices) {
            components[focusedIndex].focused = false
        }
        
        focusedIndex = if (focusedIndex <= 0) components.size - 1 else focusedIndex - 1
        components[focusedIndex].focused = true
    }
    
    /**
     * Focus a specific component.
     */
    protected fun focus(component: TuiComponent) {
        val index = components.indexOf(component)
        if (index >= 0) {
            if (focusedIndex in components.indices) {
                components[focusedIndex].focused = false
            }
            focusedIndex = index
            components[focusedIndex].focused = true
        }
    }
    
    /**
     * Set status bar text.
     */
    protected fun setStatus(left: String = "", center: String = "", right: String = "") {
        statusBar?.leftText = left
        statusBar?.centerText = center
        statusBar?.rightText = right
    }
    
    /**
     * Show a simple message dialog.
     */
    protected fun showMessage(title: String, message: String) {
        val panel = TuiPanel(
            x = (renderer.getWidth() - 40) / 2,
            y = (renderer.getHeight() - 6) / 2,
            width = 40,
            height = 6,
            title = title
        )
        
        val label = TuiLabel(1, 1, message)
        panel.add(label)
        
        val okButton = TuiButton(15, 3, "OK") {
            removeComponent(panel)
        }
        okButton.focused = true
        panel.add(okButton)
        
        addComponent(panel)
        focus(panel)
    }
}

/**
 * Simple file browser TUI app.
 */
public class FileBrowserApp(
    private val startPath: String = ".",
    private val onSelect: (String) -> Unit = {}
) : TuiApp("File Browser") {
    
    private lateinit var pathLabel: TuiLabel
    private lateinit var fileList: TuiList
    private var currentPath: String = startPath
    
    override fun onCreate() {
        pathLabel = TuiLabel(1, 1, "Path: $currentPath", TuiRenderer.Color.INFO)
        addComponent(pathLabel)
        
        fileList = TuiList(1, 3, renderer.getWidth() - 2, renderer.getHeight() - 6)
        fileList.focused = true
        fileList.onSelect = { _, item ->
            if (item == "..") {
                navigateUp()
            } else if (item.endsWith("/")) {
                navigateInto(item.dropLast(1))
            } else {
                onSelect("$currentPath/$item")
                quit()
            }
        }
        addComponent(fileList)
        focusedIndex = 1
        
        loadDirectory()
        setStatus("File Browser", "", "Enter=select q=quit")
    }
    
    private fun loadDirectory() {
        val entries = mutableListOf<String>()
        if (currentPath != "/" && currentPath != ".") {
            entries.add("..")
        }
        
        try {
            val dir = java.io.File(currentPath)
            dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))?.forEach { file ->
                entries.add(if (file.isDirectory) "${file.name}/" else file.name)
            }
        } catch (_: Exception) {
            entries.add("(error reading directory)")
        }
        
        fileList.items = entries
        fileList.selectedIndex = 0
        fileList.scrollOffset = 0
        pathLabel.text = "Path: $currentPath"
    }
    
    private fun navigateUp() {
        val parent = java.io.File(currentPath).parentFile
        if (parent != null) {
            currentPath = parent.absolutePath
            loadDirectory()
        }
    }
    
    private fun navigateInto(name: String) {
        currentPath = "$currentPath/$name"
        loadDirectory()
    }
}

/**
 * Simple text viewer TUI app.
 */
public class TextViewerApp(
    private val filePath: String,
    private val content: String
) : TuiApp("View: $filePath") {
    
    private lateinit var textArea: TuiTextArea
    
    override fun onCreate() {
        textArea = TuiTextArea(0, 1, renderer.getWidth(), renderer.getHeight() - 3)
        textArea.lines.addAll(content.lines())
        textArea.focused = true
        addComponent(textArea)
        focusedIndex = 0
        
        setStatus(filePath, "Lines: ${textArea.lines.size}", "↑↓=scroll q=quit")
    }
    
    override fun onUpdate() {
        setStatus(filePath, "Line: ${textArea.scrollOffset + 1}/${textArea.lines.size}", "↑↓=scroll q=quit")
    }
}
