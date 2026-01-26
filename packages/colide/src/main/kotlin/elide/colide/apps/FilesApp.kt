/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 */

package elide.colide.apps

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Date

/**
 * File entry for display.
 */
public data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modified: Long,
    val permissions: String = ""
) {
    public fun formattedSize(): String {
        return when {
            isDirectory -> "<DIR>"
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> "${size / 1024}K"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}M"
            else -> "${size / (1024 * 1024 * 1024)}G"
        }
    }
    
    public fun formattedDate(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return fmt.format(Date(modified))
    }
}

/**
 * # Files App
 *
 * File manager for Colide OS.
 * Supports both TUI (dual-pane) and GUI modes.
 *
 * ## Features
 * - Directory navigation
 * - File operations (copy, move, delete, rename)
 * - File preview (text, images)
 * - Search and filter
 * - Bookmarks
 * - Archive support (zip)
 *
 * ## Usage
 * ```kotlin
 * val app = FilesApp()
 * app.navigate("/home")
 * app.listFiles()
 * ```
 */
public class FilesApp {
    
    private var currentPath: Path = Path.of(System.getProperty("user.home") ?: "/")
    private var secondPath: Path? = null  // For dual-pane mode
    private var selectedFiles: MutableSet<String> = mutableSetOf()
    private var bookmarks: MutableList<String> = mutableListOf()
    private var clipboard: List<String> = emptyList()
    private var clipboardOperation: String = "copy"  // "copy" or "cut"
    private var filter: String = ""
    private var showHidden: Boolean = false
    private var sortBy: String = "name"  // "name", "size", "date", "type"
    private var sortAsc: Boolean = true
    
    init {
        loadBookmarks()
    }
    
    /**
     * Navigate to a directory.
     */
    public fun navigate(path: String): Boolean {
        val target = resolvePath(path)
        return if (Files.isDirectory(target)) {
            currentPath = target.toRealPath()
            selectedFiles.clear()
            true
        } else {
            false
        }
    }
    
    /**
     * Go to parent directory.
     */
    public fun goUp(): Boolean {
        val parent = currentPath.parent
        return if (parent != null) {
            currentPath = parent
            selectedFiles.clear()
            true
        } else {
            false
        }
    }
    
    /**
     * Get current directory path.
     */
    public fun getCurrentPath(): String = currentPath.toString()
    
    /**
     * List files in current directory.
     */
    public fun listFiles(): List<FileEntry> {
        val dir = currentPath.toFile()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        
        val files = dir.listFiles()?.filter { file ->
            (showHidden || !file.name.startsWith(".")) &&
            (filter.isEmpty() || file.name.lowercase().contains(filter.lowercase()))
        } ?: emptyList()
        
        val entries = files.map { file ->
            val attrs = try {
                Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
            } catch (e: Exception) {
                null
            }
            
            FileEntry(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                size = attrs?.size() ?: 0L,
                modified = attrs?.lastModifiedTime()?.toMillis() ?: 0L,
                permissions = getPermissions(file)
            )
        }
        
        return sortEntries(entries)
    }
    
    private fun sortEntries(entries: List<FileEntry>): List<FileEntry> {
        val comparator: Comparator<FileEntry> = when (sortBy) {
            "name" -> compareBy { it.name.lowercase() }
            "size" -> compareBy { it.size }
            "date" -> compareBy { it.modified }
            "type" -> compareBy { if (it.isDirectory) "" else it.name.substringAfterLast('.', "") }
            else -> compareBy { it.name.lowercase() }
        }
        
        val sorted = if (sortAsc) entries.sortedWith(comparator) else entries.sortedWith(comparator.reversed())
        
        // Directories first
        return sorted.sortedByDescending { it.isDirectory }
    }
    
    private fun getPermissions(file: File): String {
        val r = if (file.canRead()) "r" else "-"
        val w = if (file.canWrite()) "w" else "-"
        val x = if (file.canExecute()) "x" else "-"
        return "$r$w$x"
    }
    
    private fun resolvePath(path: String): Path {
        return when {
            path.startsWith("/") -> Path.of(path)
            path.startsWith("~") -> Path.of(System.getProperty("user.home") + path.substring(1))
            else -> currentPath.resolve(path)
        }
    }
    
    /**
     * Select/deselect a file.
     */
    public fun toggleSelect(name: String) {
        if (selectedFiles.contains(name)) {
            selectedFiles.remove(name)
        } else {
            selectedFiles.add(name)
        }
    }
    
    public fun selectAll() {
        listFiles().forEach { selectedFiles.add(it.name) }
    }
    
    public fun clearSelection() {
        selectedFiles.clear()
    }
    
    public fun getSelected(): Set<String> = selectedFiles.toSet()
    
    /**
     * File operations.
     */
    public fun copy(): Boolean {
        if (selectedFiles.isEmpty()) return false
        clipboard = selectedFiles.map { currentPath.resolve(it).toString() }
        clipboardOperation = "copy"
        return true
    }
    
    public fun cut(): Boolean {
        if (selectedFiles.isEmpty()) return false
        clipboard = selectedFiles.map { currentPath.resolve(it).toString() }
        clipboardOperation = "cut"
        return true
    }
    
    public fun paste(): Int {
        if (clipboard.isEmpty()) return 0
        
        var count = 0
        for (src in clipboard) {
            val srcFile = File(src)
            val destFile = currentPath.resolve(srcFile.name).toFile()
            
            try {
                if (clipboardOperation == "copy") {
                    if (srcFile.isDirectory) {
                        srcFile.copyRecursively(destFile)
                    } else {
                        srcFile.copyTo(destFile, overwrite = false)
                    }
                } else {
                    srcFile.renameTo(destFile)
                }
                count++
            } catch (e: Exception) {
                // Skip failed files
            }
        }
        
        if (clipboardOperation == "cut") {
            clipboard = emptyList()
        }
        
        return count
    }
    
    public fun delete(confirm: Boolean = false): Int {
        if (selectedFiles.isEmpty() || !confirm) return 0
        
        var count = 0
        for (name in selectedFiles.toList()) {
            val file = currentPath.resolve(name).toFile()
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                selectedFiles.remove(name)
                count++
            } catch (e: Exception) {
                // Skip failed files
            }
        }
        return count
    }
    
    public fun rename(oldName: String, newName: String): Boolean {
        val oldFile = currentPath.resolve(oldName).toFile()
        val newFile = currentPath.resolve(newName).toFile()
        return try {
            oldFile.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }
    
    public fun createDirectory(name: String): Boolean {
        return try {
            currentPath.resolve(name).toFile().mkdir()
        } catch (e: Exception) {
            false
        }
    }
    
    public fun createFile(name: String): Boolean {
        return try {
            currentPath.resolve(name).toFile().createNewFile()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Preview file contents.
     */
    public fun preview(name: String, maxLines: Int = 50): String {
        val file = currentPath.resolve(name).toFile()
        if (!file.exists() || file.isDirectory) return ""
        
        return try {
            val lines = file.readLines().take(maxLines)
            if (file.readLines().size > maxLines) {
                lines.joinToString("\n") + "\n... (truncated)"
            } else {
                lines.joinToString("\n")
            }
        } catch (e: Exception) {
            "[Cannot preview: ${e.message}]"
        }
    }
    
    /**
     * Search for files.
     */
    public fun search(query: String, recursive: Boolean = false): List<FileEntry> {
        val results = mutableListOf<FileEntry>()
        val lowerQuery = query.lowercase()
        
        fun searchDir(dir: File, depth: Int = 0) {
            if (depth > 10) return  // Prevent too deep recursion
            
            dir.listFiles()?.forEach { file ->
                if (file.name.lowercase().contains(lowerQuery)) {
                    val attrs = try {
                        Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                    } catch (e: Exception) { null }
                    
                    results.add(FileEntry(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = attrs?.size() ?: 0L,
                        modified = attrs?.lastModifiedTime()?.toMillis() ?: 0L
                    ))
                }
                if (recursive && file.isDirectory && !file.name.startsWith(".")) {
                    searchDir(file, depth + 1)
                }
            }
        }
        
        searchDir(currentPath.toFile())
        return results.take(100)
    }
    
    /**
     * Bookmarks.
     */
    public fun addBookmark(path: String = currentPath.toString()) {
        if (!bookmarks.contains(path)) {
            bookmarks.add(path)
            saveBookmarks()
        }
    }
    
    public fun removeBookmark(path: String) {
        bookmarks.remove(path)
        saveBookmarks()
    }
    
    public fun getBookmarks(): List<String> = bookmarks.toList()
    
    private fun loadBookmarks() {
        val file = File(System.getProperty("user.home"), ".colide/files_bookmarks")
        if (file.exists()) {
            bookmarks = file.readLines().toMutableList()
        } else {
            // Default bookmarks
            bookmarks = mutableListOf(
                System.getProperty("user.home") ?: "/",
                "/",
                "/tmp"
            )
        }
    }
    
    private fun saveBookmarks() {
        val dir = File(System.getProperty("user.home"), ".colide")
        dir.mkdirs()
        File(dir, "files_bookmarks").writeText(bookmarks.joinToString("\n"))
    }
    
    /**
     * Settings.
     */
    public fun setFilter(pattern: String) { filter = pattern }
    public fun clearFilter() { filter = "" }
    public fun toggleHidden() { showHidden = !showHidden }
    public fun setSort(by: String, ascending: Boolean = true) { sortBy = by; sortAsc = ascending }
    
    /**
     * Get file info.
     */
    public fun getInfo(name: String): Map<String, Any> {
        val file = currentPath.resolve(name).toFile()
        if (!file.exists()) return emptyMap()
        
        val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        
        return mapOf(
            "name" to file.name,
            "path" to file.absolutePath,
            "size" to attrs.size(),
            "isDirectory" to file.isDirectory,
            "created" to attrs.creationTime().toMillis(),
            "modified" to attrs.lastModifiedTime().toMillis(),
            "accessed" to attrs.lastAccessTime().toMillis(),
            "readable" to file.canRead(),
            "writable" to file.canWrite(),
            "executable" to file.canExecute()
        )
    }
    
    public companion object {
        @JvmStatic
        public fun main(args: Array<String>) {
            val app = FilesApp()
            println("Colide Files v1.0 - File Manager")
            println("Type 'help' for commands, 'quit' to exit")
            println()
            
            while (true) {
                print("${app.getCurrentPath()}> ")
                val line = readLine() ?: break
                val parts = line.trim().split("\\s+".toRegex(), limit = 2)
                val cmd = parts.getOrElse(0) { "" }.lowercase()
                val arg = parts.getOrElse(1) { "" }
                
                when (cmd) {
                    "quit", "exit", "q" -> {
                        println("Goodbye!")
                        break
                    }
                    "help", "h", "?" -> printHelp()
                    "ls", "dir", "l" -> {
                        val files = app.listFiles()
                        files.forEach { f ->
                            val marker = if (app.selectedFiles.contains(f.name)) "*" else " "
                            val type = if (f.isDirectory) "/" else ""
                            println("$marker ${f.formattedDate()} ${f.formattedSize().padStart(8)} ${f.name}$type")
                        }
                        println("${files.size} items")
                    }
                    "cd" -> {
                        if (arg.isEmpty()) {
                            app.navigate(System.getProperty("user.home") ?: "/")
                        } else if (!app.navigate(arg)) {
                            println("Cannot navigate to: $arg")
                        }
                    }
                    "up", ".." -> app.goUp()
                    "pwd" -> println(app.getCurrentPath())
                    "sel", "select" -> {
                        if (arg.isEmpty()) {
                            println("Selected: ${app.getSelected().joinToString(", ")}")
                        } else {
                            app.toggleSelect(arg)
                        }
                    }
                    "selall" -> app.selectAll()
                    "clear" -> app.clearSelection()
                    "cp", "copy" -> println(if (app.copy()) "Copied ${app.clipboard.size} items" else "Nothing selected")
                    "mv", "cut" -> println(if (app.cut()) "Cut ${app.clipboard.size} items" else "Nothing selected")
                    "paste" -> println("Pasted ${app.paste()} items")
                    "rm", "del" -> {
                        if (app.selectedFiles.isEmpty()) {
                            println("Nothing selected")
                        } else {
                            print("Delete ${app.selectedFiles.size} items? (y/n) ")
                            if (readLine()?.lowercase() == "y") {
                                println("Deleted ${app.delete(confirm = true)} items")
                            }
                        }
                    }
                    "rename", "ren" -> {
                        val names = arg.split("\\s+".toRegex(), limit = 2)
                        if (names.size == 2) {
                            println(if (app.rename(names[0], names[1])) "Renamed" else "Failed")
                        } else {
                            println("Usage: rename <old> <new>")
                        }
                    }
                    "mkdir" -> println(if (app.createDirectory(arg)) "Created directory" else "Failed")
                    "touch" -> println(if (app.createFile(arg)) "Created file" else "Failed")
                    "cat", "view" -> println(app.preview(arg))
                    "find", "search" -> {
                        val results = app.search(arg, recursive = true)
                        results.forEach { println("  ${it.path}") }
                        println("Found ${results.size} items")
                    }
                    "info" -> {
                        val info = app.getInfo(arg)
                        info.forEach { (k, v) -> println("  $k: $v") }
                    }
                    "bm", "bookmark" -> {
                        if (arg.isEmpty()) {
                            app.getBookmarks().forEachIndexed { i, b -> println("  $i: $b") }
                        } else {
                            app.addBookmark()
                            println("Bookmarked: ${app.getCurrentPath()}")
                        }
                    }
                    "go" -> {
                        val idx = arg.toIntOrNull()
                        val bm = app.getBookmarks()
                        if (idx != null && idx in bm.indices) {
                            app.navigate(bm[idx])
                        } else {
                            println("Invalid bookmark index")
                        }
                    }
                    "hidden" -> {
                        app.toggleHidden()
                        println("Show hidden: ${app.showHidden}")
                    }
                    "filter" -> {
                        if (arg.isEmpty()) {
                            app.clearFilter()
                            println("Filter cleared")
                        } else {
                            app.setFilter(arg)
                            println("Filter: $arg")
                        }
                    }
                    "sort" -> {
                        app.setSort(arg.ifEmpty { "name" })
                        println("Sort by: ${app.sortBy}")
                    }
                    "" -> {}
                    else -> println("Unknown command: $cmd. Type 'help' for commands.")
                }
            }
        }
        
        private fun printHelp() {
            println("""
Colide Files v1.0 - File Manager

NAVIGATION:
  ls, dir           List files
  cd <path>         Change directory
  up, ..            Go to parent
  pwd               Print current path
  go <n>            Go to bookmark n

FILE OPERATIONS:
  sel <name>        Toggle selection
  selall            Select all
  clear             Clear selection
  cp, copy          Copy selected
  mv, cut           Cut selected
  paste             Paste from clipboard
  rm, del           Delete selected
  rename <old> <new>  Rename file
  mkdir <name>      Create directory
  touch <name>      Create empty file

VIEW:
  cat <file>        Preview file contents
  info <file>       Show file info
  find <query>      Search files recursively

SETTINGS:
  hidden            Toggle hidden files
  filter <pattern>  Filter by name
  sort <name|size|date>  Sort files
  bm                List bookmarks
  bm add            Add current to bookmarks

  help              Show this help
  quit              Exit
            """.trimIndent())
        }
    }
}
