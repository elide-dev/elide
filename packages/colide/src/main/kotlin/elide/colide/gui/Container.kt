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

/**
 * # Container Widget
 *
 * A widget that can contain other widgets.
 * Base class for Window, Panel, and other container types.
 */
public open class Container : Widget() {
    /** Child widgets */
    protected val children: MutableList<Widget> = mutableListOf()
    
    /** Currently focused child */
    private var focusedChild: Widget? = null
    
    /**
     * Add a child widget.
     */
    public fun add(child: Widget) {
        child.parent = this
        children.add(child)
    }
    
    /**
     * Remove a child widget.
     */
    public fun remove(child: Widget) {
        if (child.parent == this) {
            child.parent = null
            children.remove(child)
            if (focusedChild == child) focusedChild = null
        }
    }
    
    /**
     * Remove all children.
     */
    public fun clear() {
        children.forEach { it.parent = null }
        children.clear()
        focusedChild = null
    }
    
    /**
     * Get child at point (x, y).
     */
    public fun childAt(px: Int, py: Int): Widget? {
        return children.lastOrNull { it.visible && it.contains(px, py) }
    }
    
    /**
     * Set focus to a specific child.
     */
    public fun setFocus(child: Widget?) {
        if (focusedChild != child) {
            focusedChild?.onBlur()
            focusedChild = child
            child?.onFocus()
        }
    }
    
    /**
     * Move focus to next focusable widget.
     */
    public fun focusNext() {
        val focusables = children.filter { it.focusable && it.visible }
        if (focusables.isEmpty()) return
        
        val currentIndex = focusables.indexOf(focusedChild)
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % focusables.size
        setFocus(focusables[nextIndex])
    }
    
    override fun render() {
        if (!visible) return
        fillBackground()
        drawBorder()
        children.forEach { child ->
            if (child.visible) child.render()
        }
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        val child = childAt(mx, my)
        if (child != null && child.focusable) {
            setFocus(child)
        }
        return child?.onMouseClick(mx, my, button) ?: false
    }
    
    override fun onMouseMove(mx: Int, my: Int) {
        children.forEach { child ->
            if (child.visible && child.contains(mx, my)) {
                child.onMouseMove(mx, my)
            }
        }
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        if (keyCode == 0x09) { // Tab
            focusNext()
            invalidate()
            return true
        }
        return focusedChild?.onKeyPress(keyCode) ?: false
    }
    
    override fun onCharInput(ch: Char): Boolean {
        return focusedChild?.onCharInput(ch) ?: false
    }
}

/**
 * # Panel Widget
 *
 * A simple container with optional title.
 */
public class Panel(
    public var title: String = ""
) : Container() {
    
    init {
        showBorder = true
        backgroundColor = Colors.BACKGROUND
    }
    
    override fun render() {
        if (!visible) return
        fillBackground()
        drawBorder()
        
        if (title.isNotEmpty()) {
            Font.drawText(absoluteX + 8, absoluteY + 4, title, foregroundColor)
        }
        
        children.forEach { child ->
            if (child.visible) child.render()
        }
    }
}
