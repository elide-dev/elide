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
 * # Undo Manager
 *
 * Manages undo/redo history for text editing operations.
 * Supports grouping related edits and efficient memory usage.
 *
 * ## Usage
 * ```kotlin
 * val undoManager = UndoManager()
 * undoManager.recordEdit(EditAction.Insert(...))
 * undoManager.undo() // returns the action to undo
 * undoManager.redo() // returns the action to redo
 * ```
 */
public class UndoManager(
    private val maxHistory: Int = 100
) {
    
    /**
     * Types of edit actions.
     */
    public sealed class EditAction {
        public abstract val cursorLine: Int
        public abstract val cursorCol: Int
        
        public data class Insert(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val text: String,
            val line: Int,
            val col: Int
        ) : EditAction()
        
        public data class Delete(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val text: String,
            val line: Int,
            val col: Int
        ) : EditAction()
        
        public data class Replace(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val oldText: String,
            val newText: String,
            val line: Int,
            val col: Int
        ) : EditAction()
        
        public data class NewLine(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val line: Int
        ) : EditAction()
        
        public data class DeleteLine(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val line: Int,
            val content: String
        ) : EditAction()
        
        public data class MergeLine(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val line: Int,
            val col: Int
        ) : EditAction()
        
        public data class Batch(
            override val cursorLine: Int,
            override val cursorCol: Int,
            val actions: List<EditAction>
        ) : EditAction()
    }
    
    private val undoStack = ArrayDeque<EditAction>()
    private val redoStack = ArrayDeque<EditAction>()
    private var grouping = false
    private val currentGroup = mutableListOf<EditAction>()
    
    /**
     * Record an edit action.
     */
    public fun recordEdit(action: EditAction) {
        if (grouping) {
            currentGroup.add(action)
        } else {
            undoStack.addLast(action)
            if (undoStack.size > maxHistory) {
                undoStack.removeFirst()
            }
            redoStack.clear()
        }
    }
    
    /**
     * Start grouping edits into a single undo action.
     */
    public fun beginGroup() {
        if (!grouping) {
            grouping = true
            currentGroup.clear()
        }
    }
    
    /**
     * End grouping and commit as single undo action.
     */
    public fun endGroup(cursorLine: Int, cursorCol: Int) {
        if (grouping && currentGroup.isNotEmpty()) {
            undoStack.addLast(EditAction.Batch(cursorLine, cursorCol, currentGroup.toList()))
            if (undoStack.size > maxHistory) {
                undoStack.removeFirst()
            }
            redoStack.clear()
        }
        grouping = false
        currentGroup.clear()
    }
    
    /**
     * Cancel current group without committing.
     */
    public fun cancelGroup() {
        grouping = false
        currentGroup.clear()
    }
    
    /**
     * Get the action to undo (returns null if nothing to undo).
     */
    public fun undo(): EditAction? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.removeLast()
        redoStack.addLast(action)
        return action
    }
    
    /**
     * Get the action to redo (returns null if nothing to redo).
     */
    public fun redo(): EditAction? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.removeLast()
        undoStack.addLast(action)
        return action
    }
    
    /**
     * Check if undo is available.
     */
    public fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * Check if redo is available.
     */
    public fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Clear all history.
     */
    public fun clear() {
        undoStack.clear()
        redoStack.clear()
        currentGroup.clear()
        grouping = false
    }
    
    /**
     * Get undo stack size.
     */
    public fun undoCount(): Int = undoStack.size
    
    /**
     * Get redo stack size.
     */
    public fun redoCount(): Int = redoStack.size
}
