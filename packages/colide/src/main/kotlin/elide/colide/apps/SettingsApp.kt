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

package elide.colide.apps

import elide.colide.ColideNative
import elide.colide.tui.*

/**
 * # Settings App
 *
 * System settings application for Colide OS.
 * Manages display, network, AI, and shell preferences.
 */
public class SettingsApp : TuiApp("Settings") {
    
    private lateinit var categoryList: TuiList
    private lateinit var settingsPanel: TuiPanel
    private lateinit var valueDisplay: TuiTextArea
    
    private val categories = listOf(
        "Display",
        "Network",
        "AI Assistant",
        "Shell",
        "System Info",
        "About"
    )
    
    private val settings = mutableMapOf(
        "display.theme" to "dark",
        "display.font_size" to "16",
        "network.auto_connect" to "true",
        "network.proxy" to "",
        "ai.model" to "tinyllama",
        "ai.max_tokens" to "256",
        "ai.temperature" to "0.7",
        "shell.prompt" to "colide> ",
        "shell.history_size" to "100",
        "shell.tab_size" to "4"
    )
    
    override fun onCreate() {
        val titleLabel = TuiLabel(1, 1, "Colide OS Settings", TuiRenderer.Color.PROMPT)
        addComponent(titleLabel)
        
        categoryList = TuiList(1, 3, 18, categories.size)
        categoryList.items = categories
        categoryList.focused = true
        categoryList.onSelect = { index, _ -> showCategory(index) }
        addComponent(categoryList)
        focusedIndex = 1
        
        settingsPanel = TuiPanel(21, 2, 56, 20, categories[0])
        addComponent(settingsPanel)
        
        valueDisplay = TuiTextArea(22, 4, 54, 16)
        addComponent(valueDisplay)
        
        showCategory(0)
        setStatus("Settings", "", "↑↓=nav Enter=select q=quit")
    }
    
    private fun showCategory(index: Int) {
        settingsPanel.title = categories[index]
        valueDisplay.clear()
        
        when (index) {
            0 -> showDisplaySettings()
            1 -> showNetworkSettings()
            2 -> showAiSettings()
            3 -> showShellSettings()
            4 -> showSystemInfo()
            5 -> showAbout()
        }
    }
    
    private fun showDisplaySettings() {
        valueDisplay.appendLine("Display Settings")
        valueDisplay.appendLine("━".repeat(40))
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Theme:     ${settings["display.theme"]}")
        valueDisplay.appendLine("Font Size: ${settings["display.font_size"]}px")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Resolution: 640x480 (VESA)")
        valueDisplay.appendLine("Color Depth: 32bpp")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("[Press Enter to modify]")
    }
    
    private fun showNetworkSettings() {
        valueDisplay.appendLine("Network Settings")
        valueDisplay.appendLine("━".repeat(40))
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Auto Connect: ${settings["network.auto_connect"]}")
        valueDisplay.appendLine("Proxy:        ${settings["network.proxy"].let { if (it.isNullOrEmpty()) "(none)" else it }}")
        valueDisplay.appendLine("")
        
        val metal = ColideNative.isAvailable && ColideNative.isMetal()
        if (metal) {
            valueDisplay.appendLine("Mode: Bare Metal (Native Drivers)")
            valueDisplay.appendLine("Status: WiFi drivers loading...")
        } else {
            valueDisplay.appendLine("Mode: Hosted (OS Network Stack)")
            valueDisplay.appendLine("Status: Using host networking")
        }
    }
    
    private fun showAiSettings() {
        valueDisplay.appendLine("AI Assistant Settings")
        valueDisplay.appendLine("━".repeat(40))
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Model:       ${settings["ai.model"]}")
        valueDisplay.appendLine("Max Tokens:  ${settings["ai.max_tokens"]}")
        valueDisplay.appendLine("Temperature: ${settings["ai.temperature"]}")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Backend: llamafile (local inference)")
        valueDisplay.appendLine("Location: /zip/models/")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Available Models:")
        valueDisplay.appendLine("  • tinyllama-1.1b (default)")
        valueDisplay.appendLine("  • phi-2")
        valueDisplay.appendLine("  • mistral-7b")
    }
    
    private fun showShellSettings() {
        valueDisplay.appendLine("Shell Settings")
        valueDisplay.appendLine("━".repeat(40))
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Prompt:       ${settings["shell.prompt"]}")
        valueDisplay.appendLine("History Size: ${settings["shell.history_size"]}")
        valueDisplay.appendLine("Tab Size:     ${settings["shell.tab_size"]}")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Features:")
        valueDisplay.appendLine("  ✓ Command history")
        valueDisplay.appendLine("  ✓ Tab completion")
        valueDisplay.appendLine("  ✓ MCP tool integration")
        valueDisplay.appendLine("  ✓ AI assistant")
    }
    
    private fun showSystemInfo() {
        valueDisplay.appendLine("System Information")
        valueDisplay.appendLine("━".repeat(40))
        valueDisplay.appendLine("")
        
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMem = runtime.maxMemory() / 1024 / 1024
        
        valueDisplay.appendLine("OS:          Colide OS")
        valueDisplay.appendLine("Runtime:     Elide (GraalVM)")
        valueDisplay.appendLine("Architecture: ${System.getProperty("os.arch")}")
        valueDisplay.appendLine("Processors:  ${runtime.availableProcessors()}")
        valueDisplay.appendLine("Memory:      ${usedMem}MB / ${maxMem}MB")
        valueDisplay.appendLine("")
        
        val metal = ColideNative.isAvailable && ColideNative.isMetal()
        valueDisplay.appendLine("Boot Mode:   ${if (metal) "Bare Metal" else "Hosted"}")
        valueDisplay.appendLine("Native Lib:  ${if (ColideNative.isAvailable) "Loaded" else "Not loaded"}")
    }
    
    private fun showAbout() {
        valueDisplay.appendLine("About Colide OS")
        valueDisplay.appendLine("━".repeat(40))
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Colide OS v0.1.0")
        valueDisplay.appendLine("A True Unikernel Operating System")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Powered by:")
        valueDisplay.appendLine("  • Elide Runtime (GraalVM)")
        valueDisplay.appendLine("  • Cosmopolitan Libc (APE)")
        valueDisplay.appendLine("  • Rust Native Drivers")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("Features:")
        valueDisplay.appendLine("  • Polyglot Execution (Kotlin, JS, Python)")
        valueDisplay.appendLine("  • Local AI Inference")
        valueDisplay.appendLine("  • MCP Tool Integration")
        valueDisplay.appendLine("  • Direct Hardware Boot")
        valueDisplay.appendLine("")
        valueDisplay.appendLine("github.com/akapug/colide")
    }
    
    override fun onKeyPress(key: Int): Boolean {
        when (key) {
            '\t'.code -> {
                if (focusedIndex == 1) {
                    categoryList.focused = false
                    valueDisplay.focused = true
                    focusedIndex = 3
                } else {
                    valueDisplay.focused = false
                    categoryList.focused = true
                    focusedIndex = 1
                }
                return true
            }
        }
        return false
    }
    
    public companion object {
        @JvmStatic
        public fun main(args: Array<String>) {
            val app = SettingsApp()
            app.run()
        }
    }
}
