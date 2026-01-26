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

/**
 * # TUI Components
 *
 * Reusable UI components for TUI applications.
 */

/**
 * Base class for all TUI components.
 */
public abstract class TuiComponent(
    public var x: Int = 0,
    public var y: Int = 0,
    public var width: Int = 10,
    public var height: Int = 1
) {
    public var visible: Boolean = true
    public var focused: Boolean = false
    
    public abstract fun render(renderer: TuiRenderer)
    public open fun handleKey(key: Int): Boolean = false
}

/**
 * Text label component.
 */
public class TuiLabel(
    x: Int = 0,
    y: Int = 0,
    public var text: String = "",
    public var color: TuiRenderer.Color = TuiRenderer.Color.DEFAULT
) : TuiComponent(x, y, text.length, 1) {
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        renderer.putStringAt(x, y, text.take(width), color)
    }
}

/**
 * Button component.
 */
public class TuiButton(
    x: Int = 0,
    y: Int = 0,
    public var label: String = "Button",
    public var onClick: () -> Unit = {}
) : TuiComponent(x, y, label.length + 4, 1) {
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        val color = if (focused) TuiRenderer.Color.HIGHLIGHT else TuiRenderer.Color.DEFAULT
        renderer.putStringAt(x, y, "[ $label ]", color)
    }
    
    override fun handleKey(key: Int): Boolean {
        if (key == '\n'.code || key == ' '.code) {
            onClick()
            return true
        }
        return false
    }
}

/**
 * Text input component.
 */
public class TuiInput(
    x: Int = 0,
    y: Int = 0,
    width: Int = 20,
    public var placeholder: String = ""
) : TuiComponent(x, y, width, 1) {
    
    public var text: String = ""
    private var cursorPos: Int = 0
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        
        val color = if (focused) TuiRenderer.Color.HIGHLIGHT else TuiRenderer.Color.DEFAULT
        val displayText = if (text.isEmpty() && !focused) placeholder else text
        val padded = displayText.padEnd(width).take(width)
        
        renderer.putStringAt(x, y, padded, color)
        
        if (focused && cursorPos <= text.length) {
            val cursorX = x + cursorPos.coerceAtMost(width - 1)
            renderer.putStringAt(cursorX, y, "_", TuiRenderer.Color.PROMPT)
        }
    }
    
    override fun handleKey(key: Int): Boolean {
        when {
            key == 0x7F || key == 0x08 -> {
                if (cursorPos > 0) {
                    text = text.removeRange(cursorPos - 1, cursorPos)
                    cursorPos--
                }
            }
            key == 0x1B5B44 -> cursorPos = (cursorPos - 1).coerceAtLeast(0)
            key == 0x1B5B43 -> cursorPos = (cursorPos + 1).coerceAtMost(text.length)
            key in 0x20..0x7E -> {
                text = text.substring(0, cursorPos) + key.toChar() + text.substring(cursorPos)
                cursorPos++
            }
            else -> return false
        }
        return true
    }
    
    public fun clear() {
        text = ""
        cursorPos = 0
    }
}

/**
 * List/menu component.
 */
public class TuiList(
    x: Int = 0,
    y: Int = 0,
    width: Int = 20,
    height: Int = 5
) : TuiComponent(x, y, width, height) {
    
    public var items: List<String> = emptyList()
    public var selectedIndex: Int = 0
    public var scrollOffset: Int = 0
    public var onSelect: (Int, String) -> Unit = { _, _ -> }
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        
        val visibleItems = items.drop(scrollOffset).take(height)
        for ((i, item) in visibleItems.withIndex()) {
            val isSelected = scrollOffset + i == selectedIndex
            val color = if (isSelected && focused) {
                TuiRenderer.Color.HIGHLIGHT
            } else if (isSelected) {
                TuiRenderer.Color.INFO
            } else {
                TuiRenderer.Color.DEFAULT
            }
            val prefix = if (isSelected) "> " else "  "
            val text = (prefix + item).take(width).padEnd(width)
            renderer.putStringAt(x, y + i, text, color)
        }
    }
    
    override fun handleKey(key: Int): Boolean {
        when (key) {
            0x1B5B41, 'k'.code -> {
                if (selectedIndex > 0) {
                    selectedIndex--
                    if (selectedIndex < scrollOffset) scrollOffset = selectedIndex
                }
            }
            0x1B5B42, 'j'.code -> {
                if (selectedIndex < items.size - 1) {
                    selectedIndex++
                    if (selectedIndex >= scrollOffset + height) scrollOffset++
                }
            }
            '\n'.code, ' '.code -> {
                if (selectedIndex in items.indices) {
                    onSelect(selectedIndex, items[selectedIndex])
                }
            }
            else -> return false
        }
        return true
    }
}

/**
 * Progress bar component.
 */
public class TuiProgress(
    x: Int = 0,
    y: Int = 0,
    width: Int = 20
) : TuiComponent(x, y, width, 1) {
    
    public var progress: Float = 0f
    public var showPercent: Boolean = true
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        
        val barWidth = if (showPercent) width - 5 else width
        val filled = (barWidth * progress.coerceIn(0f, 1f)).toInt()
        val empty = barWidth - filled
        
        renderer.putStringAt(x, y, "█".repeat(filled), TuiRenderer.Color.SUCCESS)
        renderer.putString("░".repeat(empty), TuiRenderer.Color.DEFAULT)
        
        if (showPercent) {
            val pct = "${(progress * 100).toInt()}%".padStart(4)
            renderer.putString(pct, TuiRenderer.Color.INFO)
        }
    }
}

/**
 * Panel/container component with border.
 */
public class TuiPanel(
    x: Int = 0,
    y: Int = 0,
    width: Int = 40,
    height: Int = 10,
    public var title: String = ""
) : TuiComponent(x, y, width, height) {
    
    public var style: TuiRenderer.BoxStyle = TuiRenderer.BoxStyle.SINGLE
    public var children: MutableList<TuiComponent> = mutableListOf()
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        
        val borderColor = if (focused) TuiRenderer.Color.PROMPT else TuiRenderer.Color.DEFAULT
        renderer.drawBox(x, y, width, height, style, borderColor)
        
        if (title.isNotEmpty()) {
            val titleText = " $title "
            val titleX = x + (width - titleText.length) / 2
            renderer.putStringAt(titleX, y, titleText, borderColor)
        }
        
        for (child in children) {
            child.x += x + 1
            child.y += y + 1
            child.render(renderer)
            child.x -= x + 1
            child.y -= y + 1
        }
    }
    
    override fun handleKey(key: Int): Boolean {
        for (child in children) {
            if (child.focused && child.handleKey(key)) return true
        }
        return false
    }
    
    public fun add(component: TuiComponent) {
        children.add(component)
    }
}

/**
 * Text area with scrolling.
 */
public class TuiTextArea(
    x: Int = 0,
    y: Int = 0,
    width: Int = 40,
    height: Int = 10
) : TuiComponent(x, y, width, height) {
    
    public var lines: MutableList<String> = mutableListOf()
    public var scrollOffset: Int = 0
    public var wordWrap: Boolean = true
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        
        val displayLines = if (wordWrap) wrapLines() else lines
        val visible = displayLines.drop(scrollOffset).take(height)
        
        for ((i, line) in visible.withIndex()) {
            renderer.putStringAt(x, y + i, line.take(width).padEnd(width), TuiRenderer.Color.DEFAULT)
        }
        
        for (i in visible.size until height) {
            renderer.putStringAt(x, y + i, " ".repeat(width), TuiRenderer.Color.DEFAULT)
        }
    }
    
    private fun wrapLines(): List<String> {
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.length <= width) {
                result.add(line)
            } else {
                var remaining = line
                while (remaining.length > width) {
                    result.add(remaining.take(width))
                    remaining = remaining.drop(width)
                }
                if (remaining.isNotEmpty()) result.add(remaining)
            }
        }
        return result
    }
    
    override fun handleKey(key: Int): Boolean {
        when (key) {
            0x1B5B41 -> scrollOffset = (scrollOffset - 1).coerceAtLeast(0)
            0x1B5B42 -> scrollOffset = (scrollOffset + 1).coerceAtMost((lines.size - height).coerceAtLeast(0))
            0x1B5B35 -> scrollOffset = (scrollOffset - height).coerceAtLeast(0)
            0x1B5B36 -> scrollOffset = (scrollOffset + height).coerceAtMost((lines.size - height).coerceAtLeast(0))
            else -> return false
        }
        return true
    }
    
    public fun appendLine(text: String) {
        lines.add(text)
        if (lines.size > scrollOffset + height) {
            scrollOffset = lines.size - height
        }
    }
    
    public fun clear() {
        lines.clear()
        scrollOffset = 0
    }
}

/**
 * Status bar component.
 */
public class TuiStatusBar(
    y: Int = 24,
    width: Int = 80
) : TuiComponent(0, y, width, 1) {
    
    public var leftText: String = ""
    public var centerText: String = ""
    public var rightText: String = ""
    
    override fun render(renderer: TuiRenderer) {
        if (!visible) return
        
        renderer.fillRect(x, y, width, 1, ' ', TuiRenderer.Color.HIGHLIGHT)
        
        renderer.putStringAt(x + 1, y, leftText.take(width / 3), TuiRenderer.Color.HIGHLIGHT)
        
        val centerX = x + (width - centerText.length) / 2
        renderer.putStringAt(centerX, y, centerText, TuiRenderer.Color.HIGHLIGHT)
        
        val rightX = x + width - rightText.length - 1
        renderer.putStringAt(rightX.coerceAtLeast(0), y, rightText.take(width / 3), TuiRenderer.Color.HIGHLIGHT)
    }
}
