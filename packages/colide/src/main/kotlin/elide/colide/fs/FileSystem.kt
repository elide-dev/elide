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

package elide.colide.fs

import elide.colide.ColideNative

/**
 * # FileSystem
 *
 * Abstraction for file system operations in Colide OS.
 * Supports both the embedded /zip/ filesystem (Cosmopolitan APE)
 * and standard file operations when running hosted.
 *
 * ## Paths
 * - `/zip/...` - Embedded filesystem (read-only)
 * - `/tmp/...` - Temporary storage (if available)
 * - Other paths - Native filesystem (hosted mode only)
 */
public object FileSystem {
    
    /**
     * File entry information.
     */
    public data class FileEntry(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val readable: Boolean = true,
        val writable: Boolean = false,
        val executable: Boolean = false
    ) {
        val extension: String get() = name.substringAfterLast('.', "")
    }
    
    /**
     * Check if a path exists.
     */
    @JvmStatic
    public fun exists(path: String): Boolean {
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            nativeExists(path)
        } else {
            java.io.File(path).exists()
        }
    }
    
    /**
     * Check if path is a directory.
     */
    @JvmStatic
    public fun isDirectory(path: String): Boolean {
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            nativeIsDirectory(path)
        } else {
            java.io.File(path).isDirectory
        }
    }
    
    /**
     * List directory contents.
     */
    @JvmStatic
    public fun listDirectory(path: String): List<FileEntry> {
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            nativeListDirectory(path)
        } else {
            val dir = java.io.File(path)
            if (!dir.isDirectory) return emptyList()
            
            dir.listFiles()?.map { file ->
                FileEntry(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    readable = file.canRead(),
                    writable = file.canWrite(),
                    executable = file.canExecute()
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?: emptyList()
        }
    }
    
    /**
     * Read file contents as string.
     */
    @JvmStatic
    public fun readText(path: String): String? {
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            nativeReadText(path)
        } else {
            try {
                java.io.File(path).readText()
            } catch (_: Exception) {
                null
            }
        }
    }
    
    /**
     * Read file contents as bytes.
     */
    @JvmStatic
    public fun readBytes(path: String): ByteArray? {
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            nativeReadBytes(path)
        } else {
            try {
                java.io.File(path).readBytes()
            } catch (_: Exception) {
                null
            }
        }
    }
    
    /**
     * Get file size.
     */
    @JvmStatic
    public fun getSize(path: String): Long {
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            nativeGetSize(path)
        } else {
            java.io.File(path).length()
        }
    }
    
    /**
     * Write text to file (hosted mode only, /zip/ is read-only).
     */
    @JvmStatic
    public fun writeText(path: String, content: String): Boolean {
        if (path.startsWith("/zip/")) {
            return false
        }
        
        return try {
            java.io.File(path).writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }
    
    /**
     * Get the /zip/ root for embedded filesystem.
     */
    @JvmStatic
    public fun getZipRoot(): String = "/zip"
    
    /**
     * Check if path is in the embedded /zip/ filesystem.
     */
    @JvmStatic
    public fun isEmbedded(path: String): Boolean = path.startsWith("/zip/") || path == "/zip"
    
    private fun nativeExists(path: String): Boolean {
        return nativeFileExists(path)
    }
    
    private fun nativeIsDirectory(path: String): Boolean {
        return nativeFileIsDir(path)
    }
    
    private fun nativeListDirectory(path: String): List<FileEntry> {
        val entries = nativeListDir(path) ?: return emptyList()
        return entries.map { entry ->
            val parts = entry.split("|")
            if (parts.size >= 4) {
                FileEntry(
                    name = parts[0],
                    path = "$path/${parts[0]}",
                    isDirectory = parts[1] == "d",
                    size = parts[2].toLongOrNull() ?: 0,
                    executable = parts[3] == "x"
                )
            } else {
                FileEntry(
                    name = entry,
                    path = "$path/$entry",
                    isDirectory = false,
                    size = 0
                )
            }
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
    
    private fun nativeReadText(path: String): String? {
        return nativeFileReadText(path)
    }
    
    private fun nativeReadBytes(path: String): ByteArray? {
        return nativeFileReadBytes(path)
    }
    
    private fun nativeGetSize(path: String): Long {
        return nativeFileSize(path)
    }
    
    @JvmStatic
    private external fun nativeFileExists(path: String): Boolean
    
    @JvmStatic
    private external fun nativeFileIsDir(path: String): Boolean
    
    @JvmStatic
    private external fun nativeListDir(path: String): Array<String>?
    
    @JvmStatic
    private external fun nativeFileReadText(path: String): String?
    
    @JvmStatic
    private external fun nativeFileReadBytes(path: String): ByteArray?
    
    @JvmStatic
    private external fun nativeFileSize(path: String): Long
}
