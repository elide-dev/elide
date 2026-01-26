/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 */

package elide.colide.apps

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * App manifest parsed from YAML.
 */
public data class AppManifest(
    val name: String,
    val version: String,
    val language: String,
    val entry: String,
    val author: String = "Unknown",
    val description: String = "",
    val ui: UiConfig = UiConfig(),
    val permissions: List<String> = emptyList(),
    val capabilities: List<String> = emptyList()
)

public data class UiConfig(
    val tui: Boolean = true,
    val gui: Boolean = false,
    val minWidth: Int = 80,
    val minHeight: Int = 24
)

/**
 * Result of running an app.
 */
public sealed class AppResult {
    public data class Success(public val output: Any?) : AppResult()
    public data class Error(public val message: String, public val cause: Throwable? = null) : AppResult()
}

/**
 * # App Loader
 *
 * Loads and runs Colide apps from the `/apps/` directory.
 * Supports multiple languages via GraalVM polyglot.
 *
 * ## Supported Languages
 * - `js` / `javascript` - GraalJS
 * - `py` / `python` - GraalPy
 * - `rb` / `ruby` - TruffleRuby
 * - `r` - FastR
 * - `wasm` - GraalWasm
 * - `kt` / `kotlin` - Native (special handling)
 *
 * ## Usage
 * ```kotlin
 * val loader = AppLoader("/apps")
 * val apps = loader.listApps()
 * loader.run("calc")
 * ```
 */
public class AppLoader(private val appsRoot: String = "/apps") {
    
    private val context: Context by lazy {
        Context.newBuilder()
            .allowAllAccess(true)
            .allowExperimentalOptions(true)
            .option("engine.WarnInterpreterOnly", "false")
            .build()
    }
    
    private val languageMap = mapOf(
        "js" to "js",
        "javascript" to "js",
        "py" to "python",
        "python" to "python",
        "rb" to "ruby",
        "ruby" to "ruby",
        "r" to "R",
        "wasm" to "wasm"
    )
    
    /**
     * List all available apps.
     */
    public fun listApps(): List<AppManifest> {
        val root = File(appsRoot)
        if (!root.exists()) return emptyList()
        
        return root.walkTopDown()
            .filter { it.name == "manifest.yaml" || it.name == "manifest.yml" }
            .mapNotNull { parseManifest(it) }
            .toList()
    }
    
    /**
     * Find app by name.
     */
    public fun findApp(name: String): AppManifest? {
        return listApps().find { it.name == name }
    }
    
    /**
     * Run an app by name.
     */
    public fun run(name: String, args: List<String> = emptyList()): AppResult {
        val manifest = findApp(name) ?: return AppResult.Error("App not found: $name")
        return runManifest(manifest, args)
    }
    
    /**
     * Run an app from its manifest.
     */
    public fun runManifest(manifest: AppManifest, args: List<String> = emptyList()): AppResult {
        val appDir = findAppDir(manifest.name) ?: return AppResult.Error("App directory not found")
        val entryFile = File(appDir, manifest.entry)
        
        if (!entryFile.exists()) {
            return AppResult.Error("Entry file not found: ${manifest.entry}")
        }
        
        val langId = languageMap[manifest.language.lowercase()]
            ?: return AppResult.Error("Unsupported language: ${manifest.language}")
        
        return try {
            val source = Source.newBuilder(langId, entryFile)
                .name(manifest.name)
                .build()
            
            setupAppEnvironment(manifest, args)
            val result = context.eval(source)
            
            AppResult.Success(extractValue(result))
        } catch (e: Exception) {
            AppResult.Error("Failed to run ${manifest.name}: ${e.message}", e)
        }
    }
    
    /**
     * Run a file directly (without manifest).
     */
    public fun runFile(path: String, args: List<String> = emptyList()): AppResult {
        val file = File(path)
        if (!file.exists()) return AppResult.Error("File not found: $path")
        
        val langId = detectLanguage(file.extension)
            ?: return AppResult.Error("Unknown file type: ${file.extension}")
        
        return try {
            val source = Source.newBuilder(langId, file)
                .name(file.nameWithoutExtension)
                .build()
            
            val result = context.eval(source)
            AppResult.Success(extractValue(result))
        } catch (e: Exception) {
            AppResult.Error("Failed to run $path: ${e.message}", e)
        }
    }
    
    /**
     * Run code string in specified language.
     */
    public fun eval(language: String, code: String): AppResult {
        val langId = languageMap[language.lowercase()]
            ?: return AppResult.Error("Unsupported language: $language")
        
        return try {
            val result = context.eval(langId, code)
            AppResult.Success(extractValue(result))
        } catch (e: Exception) {
            AppResult.Error("Eval failed: ${e.message}", e)
        }
    }
    
    /**
     * Parse manifest.yaml file.
     */
    private fun parseManifest(file: File): AppManifest? {
        return try {
            val content = file.readText()
            parseYamlManifest(content, file.parentFile.name)
        } catch (e: Exception) {
            System.err.println("Failed to parse manifest: ${file.path}: ${e.message}")
            null
        }
    }
    
    /**
     * Simple YAML parser for manifest files.
     */
    private fun parseYamlManifest(yaml: String, fallbackName: String): AppManifest {
        val map = mutableMapOf<String, Any>()
        var currentKey = ""
        var inList = false
        val currentList = mutableListOf<String>()
        
        for (line in yaml.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            
            when {
                trimmed.startsWith("- ") && inList -> {
                    currentList.add(trimmed.removePrefix("- ").trim())
                }
                trimmed.contains(":") -> {
                    if (inList && currentList.isNotEmpty()) {
                        map[currentKey] = currentList.toList()
                        currentList.clear()
                    }
                    inList = false
                    
                    val (key, value) = trimmed.split(":", limit = 2)
                    currentKey = key.trim()
                    val v = value.trim()
                    
                    if (v.isEmpty()) {
                        inList = true
                    } else {
                        map[currentKey] = v.removeSurrounding("\"").removeSurrounding("'")
                    }
                }
            }
        }
        
        if (inList && currentList.isNotEmpty()) {
            map[currentKey] = currentList.toList()
        }
        
        @Suppress("UNCHECKED_CAST")
        return AppManifest(
            name = map["name"]?.toString() ?: fallbackName,
            version = map["version"]?.toString() ?: "1.0.0",
            language = map["language"]?.toString() ?: "js",
            entry = map["entry"]?.toString() ?: "main.js",
            author = map["author"]?.toString() ?: "Unknown",
            description = map["description"]?.toString() ?: "",
            permissions = (map["permissions"] as? List<String>) ?: emptyList(),
            capabilities = (map["capabilities"] as? List<String>) ?: emptyList()
        )
    }
    
    private fun findAppDir(name: String): File? {
        val root = File(appsRoot)
        return root.walkTopDown()
            .filter { it.isDirectory && it.name == name }
            .firstOrNull()
            ?: root.walkTopDown()
                .filter { it.name == "manifest.yaml" || it.name == "manifest.yml" }
                .map { it.parentFile }
                .find { parseManifest(File(it, "manifest.yaml"))?.name == name }
    }
    
    private fun setupAppEnvironment(manifest: AppManifest, args: List<String>) {
        val bindings = context.getBindings("js")
        bindings.putMember("__APP_NAME__", manifest.name)
        bindings.putMember("__APP_VERSION__", manifest.version)
        bindings.putMember("__APP_ARGS__", args.toTypedArray())
    }
    
    private fun detectLanguage(extension: String): String? {
        return when (extension.lowercase()) {
            "js", "mjs" -> "js"
            "py" -> "python"
            "rb" -> "ruby"
            "r" -> "R"
            "wasm" -> "wasm"
            else -> null
        }
    }
    
    private fun extractValue(value: Value): Any? {
        return when {
            value.isNull -> null
            value.isBoolean -> value.asBoolean()
            value.isNumber -> if (value.fitsInLong()) value.asLong() else value.asDouble()
            value.isString -> value.asString()
            value.hasArrayElements() -> (0 until value.arraySize).map { extractValue(value.getArrayElement(it)) }
            else -> value.toString()
        }
    }
    
    public fun close() {
        context.close()
    }
    
    public companion object {
        private var instance: AppLoader? = null
        
        @JvmStatic
        public fun getInstance(appsRoot: String = "/apps"): AppLoader {
            return instance ?: AppLoader(appsRoot).also { instance = it }
        }
    }
}
