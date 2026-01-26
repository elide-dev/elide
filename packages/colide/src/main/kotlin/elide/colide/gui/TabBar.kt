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

/**
 * # Tab Bar
 *
 * A horizontal tab bar for multi-document editing.
 * Supports tab selection, close buttons, and overflow scrolling.
 */
public class TabBar : Widget() {
    
    public data class Tab(
        val id: String,
        val title: String,
        val path: String? = null,
        var modified: Boolean = false,
        var closable: Boolean = true
    )
    
    private val tabs = mutableListOf<Tab>()
    private var activeTabIndex: Int = -1
    private var scrollOffset: Int = 0
    private var hoverTabIndex: Int = -1
    private var hoverCloseIndex: Int = -1
    
    public var onTabSelected: ((Tab) -> Unit)? = null
    public var onTabClosed: ((Tab) -> Unit)? = null
    public var onTabContextMenu: ((Tab, Int, Int) -> Unit)? = null
    
    init {
        height = TAB_HEIGHT
        backgroundColor = BG_COLOR
        foregroundColor = FG_COLOR
    }
    
    override fun render() {
        // Background
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        // Bottom border
        Vesa.fillRect(x, y + height - 1, width, 1, BORDER_COLOR)
        
        var tabX = x - scrollOffset
        
        for ((index, tab) in tabs.withIndex()) {
            val tabWidth = calculateTabWidth(tab)
            val isActive = index == activeTabIndex
            val isHover = index == hoverTabIndex
            
            if (tabX + tabWidth > x && tabX < x + width) {
                renderTab(tabX, tab, isActive, isHover, index)
            }
            
            tabX += tabWidth
        }
        
        // Overflow indicators
        if (scrollOffset > 0) {
            Font.drawText(x + 2, y + 6, "<", SCROLL_INDICATOR_COLOR)
        }
        if (tabX > x + width) {
            Font.drawText(x + width - Font.CHAR_WIDTH - 2, y + 6, ">", SCROLL_INDICATOR_COLOR)
        }
    }
    
    private fun renderTab(tabX: Int, tab: Tab, isActive: Boolean, isHover: Boolean, index: Int) {
        val tabWidth = calculateTabWidth(tab)
        
        // Tab background
        val bg = when {
            isActive -> ACTIVE_TAB_BG
            isHover -> HOVER_TAB_BG
            else -> INACTIVE_TAB_BG
        }
        Vesa.fillRect(tabX, y, tabWidth, height - 1, bg)
        
        // Active tab indicator
        if (isActive) {
            Vesa.fillRect(tabX, y + height - 2, tabWidth, 2, ACTIVE_INDICATOR_COLOR)
        }
        
        // Tab separator
        Vesa.fillRect(tabX + tabWidth - 1, y + 4, 1, height - 8, SEPARATOR_COLOR)
        
        // Modified indicator (dot before title)
        var textX = tabX + TAB_PADDING
        if (tab.modified) {
            Vesa.fillRect(textX, y + height / 2 - 2, 4, 4, MODIFIED_COLOR)
            textX += 8
        }
        
        // Tab title
        val maxTitleChars = (tabWidth - TAB_PADDING * 2 - CLOSE_BUTTON_SIZE - 8) / Font.CHAR_WIDTH
        val displayTitle = if (tab.title.length > maxTitleChars) {
            tab.title.take(maxTitleChars - 2) + ".."
        } else {
            tab.title
        }
        val fg = if (isActive) ACTIVE_TAB_FG else INACTIVE_TAB_FG
        Font.drawText(textX, y + 6, displayTitle, fg)
        
        // Close button
        if (tab.closable) {
            val closeX = tabX + tabWidth - CLOSE_BUTTON_SIZE - TAB_PADDING / 2
            val closeY = y + (height - CLOSE_BUTTON_SIZE) / 2
            val closeHover = index == hoverCloseIndex
            
            if (closeHover) {
                Vesa.fillRect(closeX - 2, closeY - 2, CLOSE_BUTTON_SIZE + 4, CLOSE_BUTTON_SIZE + 4, CLOSE_HOVER_BG)
            }
            Font.drawText(closeX, closeY, "x", if (closeHover) CLOSE_HOVER_FG else CLOSE_COLOR)
        }
    }
    
    private fun calculateTabWidth(tab: Tab): Int {
        val titleWidth = tab.title.length * Font.CHAR_WIDTH
        val modifiedWidth = if (tab.modified) 8 else 0
        val closeWidth = if (tab.closable) CLOSE_BUTTON_SIZE + TAB_PADDING / 2 else 0
        return minOf(MAX_TAB_WIDTH, maxOf(MIN_TAB_WIDTH, titleWidth + modifiedWidth + closeWidth + TAB_PADDING * 2))
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        if (my < y || my >= y + height) return false
        
        var tabX = x - scrollOffset
        
        for ((index, tab) in tabs.withIndex()) {
            val tabWidth = calculateTabWidth(tab)
            
            if (mx >= tabX && mx < tabX + tabWidth) {
                // Check close button
                if (tab.closable) {
                    val closeX = tabX + tabWidth - CLOSE_BUTTON_SIZE - TAB_PADDING / 2
                    val closeY = y + (height - CLOSE_BUTTON_SIZE) / 2
                    if (mx >= closeX - 2 && mx < closeX + CLOSE_BUTTON_SIZE + 2 &&
                        my >= closeY - 2 && my < closeY + CLOSE_BUTTON_SIZE + 2) {
                        if (button == 1) {
                            onTabClosed?.invoke(tab)
                        }
                        return true
                    }
                }
                
                // Tab selection
                if (button == 1) {
                    selectTab(index)
                } else if (button == 2) {
                    onTabContextMenu?.invoke(tab, mx, my)
                }
                return true
            }
            
            tabX += tabWidth
        }
        
        return false
    }
    
    override fun onMouseMove(mx: Int, my: Int) {
        hoverTabIndex = -1
        hoverCloseIndex = -1
        
        if (my < y || my >= y + height) return
        
        var tabX = x - scrollOffset
        
        for ((index, tab) in tabs.withIndex()) {
            val tabWidth = calculateTabWidth(tab)
            
            if (mx >= tabX && mx < tabX + tabWidth) {
                hoverTabIndex = index
                
                // Check close button hover
                if (tab.closable) {
                    val closeX = tabX + tabWidth - CLOSE_BUTTON_SIZE - TAB_PADDING / 2
                    val closeY = y + (height - CLOSE_BUTTON_SIZE) / 2
                    if (mx >= closeX - 2 && mx < closeX + CLOSE_BUTTON_SIZE + 2 &&
                        my >= closeY - 2 && my < closeY + CLOSE_BUTTON_SIZE + 2) {
                        hoverCloseIndex = index
                    }
                }
                return
            }
            
            tabX += tabWidth
        }
    }
    
    public fun addTab(tab: Tab): Int {
        tabs.add(tab)
        if (activeTabIndex < 0) {
            activeTabIndex = 0
        }
        return tabs.size - 1
    }
    
    public fun removeTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        
        tabs.removeAt(index)
        
        if (tabs.isEmpty()) {
            activeTabIndex = -1
        } else if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.size - 1
        } else if (activeTabIndex > index) {
            activeTabIndex--
        }
    }
    
    public fun removeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) removeTab(index)
    }
    
    public fun selectTab(index: Int) {
        if (index < 0 || index >= tabs.size) return
        if (index == activeTabIndex) return
        
        activeTabIndex = index
        ensureTabVisible(index)
        onTabSelected?.invoke(tabs[index])
    }
    
    public fun selectTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) selectTab(index)
    }
    
    private fun ensureTabVisible(index: Int) {
        var tabX = 0
        for (i in 0 until index) {
            tabX += calculateTabWidth(tabs[i])
        }
        val tabWidth = calculateTabWidth(tabs[index])
        
        if (tabX < scrollOffset) {
            scrollOffset = tabX
        } else if (tabX + tabWidth > scrollOffset + width) {
            scrollOffset = tabX + tabWidth - width
        }
    }
    
    public fun getActiveTab(): Tab? = if (activeTabIndex >= 0) tabs[activeTabIndex] else null
    
    public fun getActiveTabIndex(): Int = activeTabIndex
    
    public fun getTab(index: Int): Tab? = tabs.getOrNull(index)
    
    public fun getTab(id: String): Tab? = tabs.find { it.id == id }
    
    public fun getTabCount(): Int = tabs.size
    
    public fun setTabModified(id: String, modified: Boolean) {
        tabs.find { it.id == id }?.modified = modified
    }
    
    public fun setTabTitle(id: String, title: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            tabs[index] = tabs[index].copy(title = title)
        }
    }
    
    public fun getAllTabs(): List<Tab> = tabs.toList()
    
    public fun hasModifiedTabs(): Boolean = tabs.any { it.modified }
    
    public fun getModifiedTabs(): List<Tab> = tabs.filter { it.modified }
    
    public companion object {
        public const val TAB_HEIGHT: Int = 28
        
        private const val TAB_PADDING = 12
        private const val MIN_TAB_WIDTH = 80
        private const val MAX_TAB_WIDTH = 200
        private const val CLOSE_BUTTON_SIZE = 12
        
        private const val BG_COLOR = 0x00181825
        private const val FG_COLOR = 0x00cdd6f4
        private const val BORDER_COLOR = 0x00313244
        private const val SEPARATOR_COLOR = 0x00313244
        
        private const val ACTIVE_TAB_BG = 0x001e1e2e
        private const val ACTIVE_TAB_FG = 0x00cdd6f4
        private const val ACTIVE_INDICATOR_COLOR = 0x0089b4fa
        
        private const val INACTIVE_TAB_BG = 0x00181825
        private const val INACTIVE_TAB_FG = 0x006c7086
        
        private const val HOVER_TAB_BG = 0x00313244
        
        private const val MODIFIED_COLOR = 0x00f9e2af
        private const val CLOSE_COLOR = 0x006c7086
        private const val CLOSE_HOVER_BG = 0x00f38ba8
        private const val CLOSE_HOVER_FG = 0x001e1e2e
        private const val SCROLL_INDICATOR_COLOR = 0x006c7086
    }
}
