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
 * # Dialog
 *
 * Base class for modal dialogs. Provides common dialog functionality
 * including title bar, close button, and button row.
 */
public abstract class Dialog : Container() {
    
    public var title: String = "Dialog"
    public var onClose: (() -> Unit)? = null
    public var onConfirm: (() -> Unit)? = null
    public var onCancel: (() -> Unit)? = null
    
    protected var result: DialogResult = DialogResult.NONE
    
    public enum class DialogResult {
        NONE, OK, CANCEL, YES, NO
    }
    
    init {
        backgroundColor = BG_COLOR
        foregroundColor = FG_COLOR
    }
    
    override fun render() {
        // Dialog background with border
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        // Border
        Vesa.fillRect(x, y, width, 1, BORDER_COLOR)
        Vesa.fillRect(x, y + height - 1, width, 1, BORDER_COLOR)
        Vesa.fillRect(x, y, 1, height, BORDER_COLOR)
        Vesa.fillRect(x + width - 1, y, 1, height, BORDER_COLOR)
        
        // Title bar
        Vesa.fillRect(x + 1, y + 1, width - 2, TITLE_HEIGHT, TITLE_BG_COLOR)
        Font.drawText(x + PADDING, y + 6, title, TITLE_FG_COLOR)
        
        // Close button (X)
        val closeX = x + width - TITLE_HEIGHT
        Font.drawText(closeX + 8, y + 6, "X", CLOSE_COLOR)
        
        // Content area
        renderContent()
        
        // Button row
        renderButtons()
        
        // Render child widgets
        super.render()
    }
    
    protected abstract fun renderContent()
    
    protected open fun renderButtons() {
        val buttonY = y + height - BUTTON_HEIGHT - PADDING
        val buttonWidth = 80
        
        // OK/Confirm button
        val okX = x + width - buttonWidth - PADDING
        renderButton(okX, buttonY, buttonWidth, "OK", true)
        
        // Cancel button
        val cancelX = okX - buttonWidth - PADDING
        renderButton(cancelX, buttonY, buttonWidth, "Cancel", false)
    }
    
    protected fun renderButton(bx: Int, by: Int, bw: Int, text: String, primary: Boolean) {
        val bg = if (primary) PRIMARY_BTN_BG else SECONDARY_BTN_BG
        val fg = if (primary) PRIMARY_BTN_FG else SECONDARY_BTN_FG
        
        Vesa.fillRect(bx, by, bw, BUTTON_HEIGHT, bg)
        
        val textX = bx + (bw - text.length * Font.CHAR_WIDTH) / 2
        val textY = by + (BUTTON_HEIGHT - Font.CHAR_HEIGHT) / 2
        Font.drawText(textX, textY, text, fg)
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        // Close button
        if (my >= y && my < y + TITLE_HEIGHT) {
            val closeX = x + width - TITLE_HEIGHT
            if (mx >= closeX && mx < x + width) {
                close()
                return true
            }
        }
        
        // Button row
        val buttonY = y + height - BUTTON_HEIGHT - PADDING
        if (my >= buttonY && my < buttonY + BUTTON_HEIGHT) {
            val buttonWidth = 80
            val okX = x + width - buttonWidth - PADDING
            val cancelX = okX - buttonWidth - PADDING
            
            if (mx >= okX && mx < okX + buttonWidth) {
                confirm()
                return true
            }
            if (mx >= cancelX && mx < cancelX + buttonWidth) {
                cancel()
                return true
            }
        }
        
        return super.onMouseClick(mx, my, button)
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode) {
            0x1B -> { // Escape
                cancel()
                return true
            }
            0x0D -> { // Enter
                confirm()
                return true
            }
        }
        return super.onKeyPress(keyCode)
    }
    
    protected open fun close() {
        result = DialogResult.CANCEL
        onClose?.invoke()
    }
    
    protected open fun confirm() {
        result = DialogResult.OK
        onConfirm?.invoke()
        onClose?.invoke()
    }
    
    protected open fun cancel() {
        result = DialogResult.CANCEL
        onCancel?.invoke()
        onClose?.invoke()
    }
    
    public fun dialogResult(): DialogResult = result
    
    public companion object {
        public const val TITLE_HEIGHT: Int = 28
        public const val BUTTON_HEIGHT: Int = 28
        protected const val PADDING: Int = 12
        
        private const val BG_COLOR = 0x001e1e2e
        private const val FG_COLOR = 0x00cdd6f4
        private const val BORDER_COLOR = 0x00585b70
        private const val TITLE_BG_COLOR = 0x00313244
        private const val TITLE_FG_COLOR = 0x00cdd6f4
        private const val CLOSE_COLOR = 0x00f38ba8
        
        private const val PRIMARY_BTN_BG = 0x0089b4fa
        private const val PRIMARY_BTN_FG = 0x001e1e2e
        private const val SECONDARY_BTN_BG = 0x00313244
        private const val SECONDARY_BTN_FG = 0x00cdd6f4
    }
}

/**
 * # Input Dialog
 *
 * A dialog with a text input field.
 */
public class InputDialog : Dialog() {
    
    public var prompt: String = "Enter value:"
    public var inputValue: String = ""
    public var placeholder: String = ""
    
    private var cursorPos = 0
    private var cursorVisible = true
    private var cursorBlinkTime = 0L
    
    override fun renderContent() {
        val contentY = y + TITLE_HEIGHT + PADDING
        
        // Prompt text
        Font.drawText(x + PADDING, contentY, prompt, foregroundColor)
        
        // Input field
        val inputY = contentY + Font.CHAR_HEIGHT + PADDING
        val inputWidth = width - PADDING * 2
        val inputHeight = 28
        
        Vesa.fillRect(x + PADDING, inputY, inputWidth, inputHeight, INPUT_BG_COLOR)
        Vesa.fillRect(x + PADDING, inputY, inputWidth, 1, INPUT_BORDER_COLOR)
        Vesa.fillRect(x + PADDING, inputY + inputHeight - 1, inputWidth, 1, INPUT_BORDER_COLOR)
        Vesa.fillRect(x + PADDING, inputY, 1, inputHeight, INPUT_BORDER_COLOR)
        Vesa.fillRect(x + PADDING + inputWidth - 1, inputY, 1, inputHeight, INPUT_BORDER_COLOR)
        
        // Input text or placeholder
        val textY = inputY + (inputHeight - Font.CHAR_HEIGHT) / 2
        if (inputValue.isEmpty() && placeholder.isNotEmpty()) {
            Font.drawText(x + PADDING + 4, textY, placeholder, PLACEHOLDER_COLOR)
        } else {
            Font.drawText(x + PADDING + 4, textY, inputValue, foregroundColor)
            
            // Cursor
            if (cursorVisible && focused) {
                val cursorX = x + PADDING + 4 + cursorPos * Font.CHAR_WIDTH
                Vesa.fillRect(cursorX, textY, 2, Font.CHAR_HEIGHT, foregroundColor)
            }
        }
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode) {
            0x08 -> { // Backspace
                if (cursorPos > 0) {
                    inputValue = inputValue.substring(0, cursorPos - 1) + inputValue.substring(cursorPos)
                    cursorPos--
                }
                return true
            }
            0x7F -> { // Delete
                if (cursorPos < inputValue.length) {
                    inputValue = inputValue.substring(0, cursorPos) + inputValue.substring(cursorPos + 1)
                }
                return true
            }
            0x25 -> { // Left arrow
                if (cursorPos > 0) cursorPos--
                return true
            }
            0x27 -> { // Right arrow
                if (cursorPos < inputValue.length) cursorPos++
                return true
            }
            0x24 -> { // Home
                cursorPos = 0
                return true
            }
            0x23 -> { // End
                cursorPos = inputValue.length
                return true
            }
            in 0x20..0x7E -> { // Printable characters
                val char = keyCode.toChar()
                inputValue = inputValue.substring(0, cursorPos) + char + inputValue.substring(cursorPos)
                cursorPos++
                return true
            }
        }
        return super.onKeyPress(keyCode)
    }
    
    public fun getValue(): String = inputValue
    
    public fun setValue(value: String) {
        inputValue = value
        cursorPos = value.length
    }
    
    private companion object {
        private const val INPUT_BG_COLOR = 0x00181825
        private const val INPUT_BORDER_COLOR = 0x00585b70
        private const val PLACEHOLDER_COLOR = 0x006c7086
    }
}

/**
 * # Confirm Dialog
 *
 * A dialog for yes/no/cancel confirmations.
 */
public class ConfirmDialog : Dialog() {
    
    public var message: String = "Are you sure?"
    public var showCancel: Boolean = true
    
    public var onYes: (() -> Unit)? = null
    public var onNo: (() -> Unit)? = null
    
    override fun renderContent() {
        val contentY = y + TITLE_HEIGHT + PADDING * 2
        
        // Message (can be multi-line)
        val lines = message.split("\n")
        var lineY = contentY
        for (line in lines) {
            Font.drawText(x + PADDING, lineY, line, foregroundColor)
            lineY += Font.CHAR_HEIGHT + 4
        }
    }
    
    override fun renderButtons() {
        val buttonY = y + height - BUTTON_HEIGHT - PADDING
        val buttonWidth = 80
        var currentX = x + width - PADDING
        
        // Yes button
        currentX -= buttonWidth
        renderButton(currentX, buttonY, buttonWidth, "Yes", true)
        
        // No button
        currentX -= buttonWidth + PADDING
        renderButton(currentX, buttonY, buttonWidth, "No", false)
        
        // Cancel button (optional)
        if (showCancel) {
            currentX -= buttonWidth + PADDING
            renderButton(currentX, buttonY, buttonWidth, "Cancel", false)
        }
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        val buttonY = y + height - BUTTON_HEIGHT - PADDING
        if (my >= buttonY && my < buttonY + BUTTON_HEIGHT) {
            val buttonWidth = 80
            var currentX = x + width - PADDING
            
            // Yes button
            currentX -= buttonWidth
            if (mx >= currentX && mx < currentX + buttonWidth) {
                result = DialogResult.YES
                onYes?.invoke()
                onClose?.invoke()
                return true
            }
            
            // No button
            currentX -= buttonWidth + PADDING
            if (mx >= currentX && mx < currentX + buttonWidth) {
                result = DialogResult.NO
                onNo?.invoke()
                onClose?.invoke()
                return true
            }
            
            // Cancel button
            if (showCancel) {
                currentX -= buttonWidth + PADDING
                if (mx >= currentX && mx < currentX + buttonWidth) {
                    cancel()
                    return true
                }
            }
        }
        
        return super.onMouseClick(mx, my, button)
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode.toChar().lowercaseChar()) {
            'y' -> {
                result = DialogResult.YES
                onYes?.invoke()
                onClose?.invoke()
                return true
            }
            'n' -> {
                result = DialogResult.NO
                onNo?.invoke()
                onClose?.invoke()
                return true
            }
        }
        return super.onKeyPress(keyCode)
    }
}

/**
 * # File Dialog
 *
 * A dialog for file selection (save as, open).
 */
public class FileDialog : Dialog() {
    
    public enum class Mode { OPEN, SAVE }
    
    public var mode: Mode = Mode.SAVE
    public var currentPath: String = "/"
    public var filename: String = ""
    public var filter: String = "*" // e.g., "*.kt"
    
    public var onFileSelected: ((String) -> Unit)? = null
    
    private val files = mutableListOf<String>()
    private var selectedIndex = -1
    private var scrollOffset = 0
    
    init {
        width = 400
        height = 350
    }
    
    override fun renderContent() {
        val contentY = y + TITLE_HEIGHT + PADDING
        
        // Current path
        Font.drawText(x + PADDING, contentY, "Location: $currentPath", DIMMED_COLOR)
        
        // File list area
        val listY = contentY + Font.CHAR_HEIGHT + PADDING
        val listHeight = height - TITLE_HEIGHT - BUTTON_HEIGHT - PADDING * 5 - Font.CHAR_HEIGHT * 2
        val listWidth = width - PADDING * 2
        
        Vesa.fillRect(x + PADDING, listY, listWidth, listHeight, LIST_BG_COLOR)
        
        // File entries
        val visibleItems = listHeight / ITEM_HEIGHT
        var itemY = listY
        for (i in scrollOffset until minOf(scrollOffset + visibleItems, files.size)) {
            val file = files[i]
            val isSelected = i == selectedIndex
            
            if (isSelected) {
                Vesa.fillRect(x + PADDING, itemY, listWidth, ITEM_HEIGHT, SELECTED_BG_COLOR)
            }
            
            Font.drawText(x + PADDING + 4, itemY + 4, file, if (isSelected) SELECTED_FG_COLOR else foregroundColor)
            itemY += ITEM_HEIGHT
        }
        
        // Filename input (for save mode)
        if (mode == Mode.SAVE) {
            val inputY = listY + listHeight + PADDING
            Font.drawText(x + PADDING, inputY, "Filename:", foregroundColor)
            
            val inputFieldY = inputY + Font.CHAR_HEIGHT + 4
            val inputWidth = listWidth
            Vesa.fillRect(x + PADDING, inputFieldY, inputWidth, 24, INPUT_BG_COLOR)
            Font.drawText(x + PADDING + 4, inputFieldY + 4, filename, foregroundColor)
        }
    }
    
    public fun setFiles(fileList: List<String>) {
        files.clear()
        files.addAll(fileList)
        selectedIndex = -1
        scrollOffset = 0
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        val contentY = y + TITLE_HEIGHT + PADDING
        val listY = contentY + Font.CHAR_HEIGHT + PADDING
        val listHeight = height - TITLE_HEIGHT - BUTTON_HEIGHT - PADDING * 5 - Font.CHAR_HEIGHT * 2
        
        // Click in file list
        if (mx >= x + PADDING && mx < x + width - PADDING && my >= listY && my < listY + listHeight) {
            val clickedIndex = scrollOffset + (my - listY) / ITEM_HEIGHT
            if (clickedIndex < files.size) {
                selectedIndex = clickedIndex
                filename = files[clickedIndex]
                return true
            }
        }
        
        return super.onMouseClick(mx, my, button)
    }
    
    override fun confirm() {
        if (filename.isNotEmpty()) {
            val fullPath = if (currentPath.endsWith("/")) {
                currentPath + filename
            } else {
                "$currentPath/$filename"
            }
            onFileSelected?.invoke(fullPath)
        }
        super.confirm()
    }
    
    private companion object {
        private const val ITEM_HEIGHT = 20
        private const val LIST_BG_COLOR = 0x00181825
        private const val INPUT_BG_COLOR = 0x00181825
        private const val SELECTED_BG_COLOR = 0x0089b4fa
        private const val SELECTED_FG_COLOR = 0x001e1e2e
        private const val DIMMED_COLOR = 0x006c7086
    }
}
