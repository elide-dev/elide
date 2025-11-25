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

package elide.runtime.lang.javascript

import com.oracle.truffle.api.TruffleFile
import com.oracle.truffle.api.TruffleLanguage
import com.oracle.truffle.js.runtime.JSRealm
import com.oracle.truffle.js.runtime.objects.JSDynamicObject
import com.oracle.truffle.js.runtime.objects.JSObject
import com.oracle.truffle.js.runtime.objects.Null
import com.oracle.truffle.js.runtime.objects.Undefined
import com.oracle.truffle.js.builtins.commonjs.CommonJSResolution

/**
 * Resolves package.json "exports" field according to Node.js conditional exports specification.
 *
 * This handles nested conditional exports like:
 * ```json
 * {
 *   "exports": {
 *     ".": {
 *       "import": {
 *         "types": "./dist/index.d.mts",
 *         "default": "./dist/index.mjs"
 *       },
 *       "require": {
 *         "types": "./dist/index.d.ts",
 *         "default": "./dist/index.js"
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * @see <a href="https://nodejs.org/api/packages.html#conditional-exports">Node.js Conditional Exports</a>
 */
internal object PackageExportsResolver {
  private const val NODE_MODULES = "node_modules"
  private const val PACKAGE_JSON = "package.json"
  private const val EXPORTS_PROPERTY = "exports"
  
  // Default conditions for ESM imports, in priority order
  private val ESM_CONDITIONS = listOf("import", "module", "default")
  
  /**
   * Try to resolve an npm package specifier using package.json exports.
   *
   * @param specifier The package specifier (e.g., "@discordjs/collection" or "lodash/get")
   * @param parentPath The path of the importing module
   * @param env The Truffle environment
   * @param realm The JS realm
   * @return The resolved file, or null if exports couldn't resolve it
   */
  fun tryResolveWithExports(
    specifier: String,
    parentPath: TruffleFile,
    env: TruffleLanguage.Env,
    realm: JSRealm,
  ): TruffleFile? {
    // Don't handle relative or absolute paths
    if (specifier.startsWith(".") || specifier.startsWith("/")) {
      return null
    }
    
    // Parse package name and subpath
    val (packageName, subpath) = parsePackageSpecifier(specifier)
    
    // Walk up directory tree looking for node_modules
    var current: TruffleFile? = parentPath.parent
    while (current != null) {
      val nodeModulesDir = current.resolve(NODE_MODULES)
      if (nodeModulesDir.exists() && nodeModulesDir.isDirectory) {
        val packageDir = nodeModulesDir.resolve(packageName)
        if (packageDir.exists() && packageDir.isDirectory) {
          val resolved = resolvePackageExports(packageDir, subpath, env, realm)
          if (resolved != null && resolved.exists() && !resolved.isDirectory) {
            return resolved
          }
        }
      }
      current = current.parent
    }
    
    return null
  }
  
  /**
   * Parse a package specifier into package name and subpath.
   * 
   * Examples:
   * - "lodash" -> ("lodash", ".")
   * - "lodash/get" -> ("lodash", "./get")
   * - "@discordjs/collection" -> ("@discordjs/collection", ".")
   * - "@discordjs/collection/dist" -> ("@discordjs/collection", "./dist")
   */
  private fun parsePackageSpecifier(specifier: String): Pair<String, String> {
    val parts = specifier.split("/")
    
    return if (specifier.startsWith("@") && parts.size >= 2) {
      // Scoped package: @scope/name or @scope/name/subpath
      val packageName = "${parts[0]}/${parts[1]}"
      val subpath = if (parts.size > 2) {
        "./" + parts.drop(2).joinToString("/")
      } else {
        "."
      }
      packageName to subpath
    } else {
      // Regular package: name or name/subpath
      val packageName = parts[0]
      val subpath = if (parts.size > 1) {
        "./" + parts.drop(1).joinToString("/")
      } else {
        "."
      }
      packageName to subpath
    }
  }
  
  /**
   * Resolve exports from a package directory.
   */
  private fun resolvePackageExports(
    packageDir: TruffleFile,
    subpath: String,
    env: TruffleLanguage.Env,
    realm: JSRealm,
  ): TruffleFile? {
    val packageJsonFile = packageDir.resolve(PACKAGE_JSON)
    if (!packageJsonFile.exists()) {
      return null
    }
    
    val packageJson = try {
      CommonJSResolution.loadJsonObject(packageJsonFile, realm)
    } catch (e: Exception) {
      return null
    }
    
    if (packageJson == null || !JSObject.hasProperty(packageJson, EXPORTS_PROPERTY)) {
      return null
    }
    
    val exports = JSObject.get(packageJson, EXPORTS_PROPERTY)
    if (exports == null || exports == Null.instance || exports == Undefined.instance) {
      return null
    }
    
    val resolvedPath = resolveExportsTarget(exports, subpath, ESM_CONDITIONS)
      ?: return null
    
    // Resolve the path relative to package directory
    val normalizedPath = resolvedPath.removePrefix("./")
    return packageDir.resolve(normalizedPath)
  }
  
  /**
   * Resolve an exports target according to Node.js algorithm.
   * 
   * The target can be:
   * - A string: "./dist/index.mjs"
   * - An object with conditions: { "import": "./index.mjs", "require": "./index.js" }
   * - An object with subpaths: { ".": "./index.js", "./sub": "./sub.js" }
   * - Nested conditions: { "import": { "types": "./index.d.ts", "default": "./index.js" } }
   */
  private fun resolveExportsTarget(
    target: Any?,
    subpath: String,
    conditions: List<String>,
  ): String? {
    return when {
      // Null/undefined - no resolution
      target == null || target == Null.instance || target == Undefined.instance -> null
      
      // String target - direct path
      target is String -> target
      
      // TruffleString - convert and return
      target is com.oracle.truffle.api.strings.TruffleString -> target.toJavaStringUncached()
      
      // Object target - could be conditions or subpaths
      target is JSDynamicObject -> resolveExportsObject(target, subpath, conditions)
      
      // Unknown type
      else -> null
    }
  }
  
  /**
   * Resolve an exports object, handling both condition maps and subpath maps.
   */
  private fun resolveExportsObject(
    obj: JSDynamicObject,
    subpath: String,
    conditions: List<String>,
  ): String? {
    // Check if this is a subpath map (keys start with ".") or condition map
    val keys = getObjectKeys(obj)
    val hasSubpaths = keys.any { it.startsWith(".") }
    val hasConditions = keys.any { !it.startsWith(".") }
    
    // Node.js spec: can't mix subpaths and conditions at same level
    // If mixed, treat as conditions
    
    return if (hasSubpaths && !hasConditions) {
      // This is a subpath map - look up the subpath
      val value = JSObject.get(obj, subpath)
      if (value != null && value != Null.instance && value != Undefined.instance) {
        resolveExportsTarget(value, ".", conditions)
      } else {
        // Try pattern matching (e.g., "./*" patterns) - not implemented yet
        null
      }
    } else {
      // This is a condition map - check conditions in order
      for (condition in conditions) {
        if (JSObject.hasProperty(obj, condition)) {
          val value = JSObject.get(obj, condition)
          val resolved = resolveExportsTarget(value, subpath, conditions)
          if (resolved != null) {
            return resolved
          }
        }
      }
      
      // Try "default" as fallback if not in conditions list
      if ("default" !in conditions && JSObject.hasProperty(obj, "default")) {
        val value = JSObject.get(obj, "default")
        return resolveExportsTarget(value, subpath, conditions)
      }
      
      null
    }
  }
  
  /**
   * Get the keys of a JS object.
   */
  private fun getObjectKeys(obj: JSDynamicObject): List<String> {
    return try {
      JSObject.enumerableOwnNames(obj).mapNotNull { key ->
        when (key) {
          is String -> key
          is com.oracle.truffle.api.strings.TruffleString -> key.toJavaStringUncached()
          else -> null
        }
      }
    } catch (e: Exception) {
      emptyList()
    }
  }
}
