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
package elide.runtime.gvm.internals

import com.oracle.js.parser.ir.Module.ModuleRequest
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.TruffleFile
import com.oracle.truffle.api.TruffleLanguage.Env
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.api.strings.TruffleString
import com.oracle.truffle.js.builtins.commonjs.CommonJSRequireBuiltin
import com.oracle.truffle.js.builtins.commonjs.CommonJSResolution
import com.oracle.truffle.js.lang.JavaScriptLanguage
import com.oracle.truffle.js.runtime.*
import com.oracle.truffle.js.runtime.JSErrorType.TypeError
import com.oracle.truffle.js.runtime.builtins.JSFunction
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject
import com.oracle.truffle.js.runtime.objects.*
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import elide.runtime.gvm.internals.AbstractJsModuleLoader.Format.CommonJS
import elide.runtime.gvm.internals.AbstractJsModuleLoader.Format.ESM

/**
 * ## Abstract JavaScript Module Loader
 */
internal abstract class AbstractJsModuleLoader protected constructor(realm: JSRealm) : DefaultESModuleLoader(realm) {
  /**
   * Node.js-compatible implementation of ES modules loading.
   *
   * @see [ES Modules](https://nodejs.org/api/esm.html.esm_import_specifiers)
   *
   * @see [Resolver algorithm](https://nodejs.org/api/esm.html.esm_resolver_algorithm)
   *
   *
   * @param referencingModule Referencing ES Module.
   * @param moduleRequest ES Modules Request.
   * @return ES Module record for this module.
   */
  @TruffleBoundary override fun resolveImportedModule(
    referencingModule: ScriptOrModule,
    moduleRequest: ModuleRequest
  ): JSModuleRecord {
    val specifier = moduleRequest.specifier.toJavaStringUncached()
    CommonJSRequireBuiltin.log("IMPORT resolve ", specifier)
    val moduleReplacementName = CommonJSResolution.getCoreModuleReplacement(realm, specifier)
    if (moduleReplacementName != null) {
      return loadCoreModuleReplacement(referencingModule, moduleRequest, moduleReplacementName)
    }
    try {
      val env = realm.env
      val parentURL = getFullPath(referencingModule).toUri()
      val resolution = esmResolve(specifier, parentURL, env)
      if (resolution === TryCommonJS) {
        // Compatibility mode: try loading as a CommonJS module.
        return tryLoadingAsCommonjsModule(specifier)
      } else {
        if (resolution === TryCustomESM) {
          // Failed ESM resolution. Give the virtual FS a chance to map to a file.
          // A custom Truffle FS might still try to map a package specifier to some file.
          val maybeFile = env.getPublicTruffleFile(specifier)
          if (maybeFile.exists() && !maybeFile.isDirectory()) {
            return loadModuleFromUrl(referencingModule, moduleRequest, maybeFile, maybeFile.path)
          }
        } else if (resolution != null) {
          val file = env.getPublicTruffleFile(resolution)
          return loadModuleFromUrl(referencingModule, moduleRequest, file, file.path)
        }
      }
      // Really could not load as ESM.
      throw fail(MODULE_NOT_FOUND, specifier)
    } catch (e: IOException) {
      CommonJSRequireBuiltin.log("IMPORT resolve ", specifier, " FAILED ", e.message)
      throw Errors.createErrorFromException(e)
    } catch (e: SecurityException) {
      CommonJSRequireBuiltin.log("IMPORT resolve ", specifier, " FAILED ", e.message)
      throw Errors.createErrorFromException(e)
    } catch (e: UnsupportedOperationException) {
      CommonJSRequireBuiltin.log("IMPORT resolve ", specifier, " FAILED ", e.message)
      throw Errors.createErrorFromException(e)
    } catch (e: IllegalArgumentException) {
      CommonJSRequireBuiltin.log("IMPORT resolve ", specifier, " FAILED ", e.message)
      throw Errors.createErrorFromException(e)
    }
  }

  private fun loadCoreModuleReplacement(
    referencingModule: ScriptOrModule,
    moduleRequest: ModuleRequest,
    moduleReplacementName: String
  ): JSModuleRecord {
    val specifier = moduleRequest.specifier.toJavaStringUncached()
    CommonJSRequireBuiltin.log("IMPORT resolve built-in ", specifier)
    val existingModule = moduleMap[specifier]
    if (existingModule != null) {
      CommonJSRequireBuiltin.log("IMPORT resolve built-in from cache ", specifier)
      return existingModule
    }
    val src: Source
    if (moduleReplacementName.endsWith(CommonJSResolution.MJS_EXT)) {
      val maybeUri = asURI(moduleReplacementName)
      if (maybeUri != null) {
        // Load from URI
        val env = realm.env
        val parentURL = getFullPath(referencingModule).toUri()
        val resolution = checkNotNull(esmResolve(moduleReplacementName, parentURL, env))
        try {
          val file = env.getPublicTruffleFile(resolution)
          return loadModuleFromUrl(referencingModule, moduleRequest, file, file.path)
        } catch (e: IOException) {
          throw fail(FAILED_BUILTIN, specifier)
        }
      } else {
        // Just load the module
        try {
          val cwdOption = realm.contextOptions.requireCwd
          val cwd =
            if (cwdOption.isEmpty()) realm.env.currentWorkingDirectory else realm.env.getPublicTruffleFile(cwdOption)
          val modulePath = CommonJSResolution.joinPaths(cwd, moduleReplacementName)
          src = Source.newBuilder(JavaScriptLanguage.ID, modulePath).build()
        } catch (e: IOException) {
          throw fail(FAILED_BUILTIN, specifier)
        } catch (e: SecurityException) {
          throw fail(FAILED_BUILTIN, specifier)
        }
      }
    } else {
      // Else, try loading as commonjs built-in module replacement
      return tryLoadingAsCommonjsModule(specifier)
    }
    val parsedModule = realm.context.evaluator.envParseModule(realm, src)
    val record = JSModuleRecord(parsedModule, this)
    moduleMap[specifier] = record
    return record
  }

  private fun tryLoadingAsCommonjsModule(specifier: String): JSModuleRecord {
    val existingModule = moduleMap[specifier]
    if (existingModule != null) {
      CommonJSRequireBuiltin.log("IMPORT resolve built-in from cache ", specifier)
      return existingModule
    }
    val require = realm.commonJSRequireFunctionObject as JSFunctionObject
    // Any exception thrown during module loading will be propagated
    val maybeModule =
      JSFunction.call(JSArguments.create(Undefined.instance, require, Strings.fromJavaString(specifier)))
    if (maybeModule === Undefined.instance || !JSDynamicObject.isJSDynamicObject(maybeModule)) {
      throw fail(FAILED_BUILTIN, specifier)
    }
    val module = maybeModule as JSDynamicObject
    // Wrap any exported symbol in an ES module.
    val exportedValues = JSObject.enumerableOwnNames(module)
    val moduleBody = Strings.builderCreate()
    Strings.builderAppend(moduleBody, "const builtinModule = require('")
    Strings.builderAppend(moduleBody, specifier)
    Strings.builderAppend(moduleBody, "');\n")
    for (s in exportedValues) {
      Strings.builderAppend(moduleBody, "export const ")
      Strings.builderAppend(moduleBody, s)
      Strings.builderAppend(moduleBody, " = builtinModule.")
      Strings.builderAppend(moduleBody, s)
      Strings.builderAppend(moduleBody, ";\n")
    }
    Strings.builderAppend(moduleBody, "export default builtinModule;")
    val src = Source.newBuilder(
      JavaScriptLanguage.ID, Strings.builderToJavaString(moduleBody),
      "$specifier-internal.mjs",
    ).build()
    val parsedModule = realm.context.evaluator.envParseModule(realm, src)
    val record = JSModuleRecord(parsedModule, this)
    moduleMap[specifier] = record
    return record
  }

  //
  // #### ESM resolution algorithm emulation.
  //
  // Best-effort implementation based on Node.js' v16.15.0 resolution algorithm.
  //
  /**
   * ESM_RESOLVE(specifier, parentURL).
   */
  private fun esmResolve(specifier: String, parentURL: URI, env: Env): URI? {
    // 1. Let resolved be undefined.
    var resolved = asURI(specifier)
    // 2. If specifier is a valid URL, then
    // 2.1 Set resolved to the result of parsing and reserializing specifier as a URL.
    if (resolved == null) {
      resolved =
        if (!specifier.isEmpty() && specifier[0] == '/' || isRelativePathFileName(specifier)) {
          // 3. Otherwise, if specifier starts with "/", "./" or "../", then
          // 3.1 Set resolved to the URL resolution of specifier relative to parentURL.
          resolveRelativeToParent(specifier, parentURL)
        } else if (!specifier.isEmpty() && specifier[0] == '#') {
          // 4. Otherwise, if specifier starts with "#", then
          throw fail(UNSUPPORTED_PACKAGE_IMPORTS, specifier)
        } else {
          // 5.1 Note: specifier is now a bare specifier.
          // 5.2 Set resolvedURL the result of PACKAGE_RESOLVE(specifier, parentURL).
          packageResolve(specifier, parentURL, env)
        }
    }
    if (resolved == null) {
      // package was not found: will try loading as CommonJS module instead
      return TryCommonJS
    } else if (resolved === TryCommonJS || resolved === TryCustomESM) {
      // Try customFS lookup
      return resolved
    }
    // 6. Let format be undefined.
    val format: Format
    // 7. If resolved is a "file:" URL, then
    if (isFileURI(resolved)) {
      // 7.1 If resolvedURL contains any percent encodings of "/" or "\" ("%2f" and "%5C"
      // respectively), then
      if (resolved.toString().uppercase(Locale.getDefault()).contains("%2F") || resolved.toString().uppercase(
          Locale.getDefault(),
        ).contains("%5C")
      ) {
        // 7.1.1 Throw an Invalid Module Specifier error.
        throw fail(INVALID_MODULE_SPECIFIER, specifier)
      }
      // 7.2 If the file at resolved is a directory, then
      if (isDirectory(resolved, env)) {
        // 7.2.1 Throw an Unsupported Directory Import error.
        throw fail(UNSUPPORTED_DIRECTORY_IMPORT, specifier)
      }
      // 7.3 If the file at resolved does not exist, then
      if (!fileExists(resolved, env)) {
        // 7.3.1 Throw a Module Not Found error.
        throw fail(MODULE_NOT_FOUND, specifier)
      }
      // 7.4 Set resolved to the real path of resolved, maintaining the same URL querystring
      // and fragment components.
      resolved = resolved.normalize()
      // 7.5 Set format to the result of ESM_FILE_FORMAT(resolved).
      format = esmFileFormat(resolved, env)
    } else {
      // 8. Otherwise
      // 8.1 Set format the module format of the content type associated with the URL
      // resolved.
      format = getAssociatedDefaultFormat(resolved)
    }
    return if (format == CommonJS) {
      // Will load as CommonJS.
      TryCommonJS
    } else {
      // Will load as ESM.
      resolved
    }
  }

  /**
   * ESM_FILE_FORMAT(url).
   */
  private fun esmFileFormat(url: URI?, env: Env): Format {
    // 1. Assert: url corresponds to an existing file.
    assert(fileExists(url, env))
    // 2. If url ends in ".mjs", Return "module".
    if (url!!.path.endsWith(CommonJSResolution.MJS_EXT)) {
      return ESM
    }
    // 3. If url ends in ".cjs", Return "module".
    if (url.path.endsWith(CommonJSResolution.CJS_EXT)) {
      // Note: we will try loading as CJS like Node.js does.
      return CommonJS
    }
    // 4. If url ends in ".mjs", Return "module".
    if (url.path.endsWith(CommonJSResolution.JSON_EXT)) {
      throw failMessage(UNSUPPORTED_JSON)
    }
    // 5. Let packageURL be the result of LOOKUP_PACKAGE_SCOPE(url).
    val packageUri = lookupPackageScope(url, env)
    if (packageUri != null) {
      // 6. Let pjson be the result of READ_PACKAGE_JSON(packageURL).
      val pjson = readPackageJson(packageUri, env)
      // 7. If pjson?.type exists and is "module", then
      if (pjson != null && pjson.hasTypeModule()) {
        // 7.1 If url ends in ".js", then Return "module"
        if (url.path.endsWith(CommonJSResolution.JS_EXT)) {
          return ESM
        }
      }
    } else if (url.path.endsWith(CommonJSResolution.JS_EXT)) {
      // Np Package.json with .js extension: try loading as CJS like Node.js does.
      return CommonJS
    }
    // 8. Otherwise, Throw an Unsupported File Extension error.
    throw fail(UNSUPPORTED_FILE_EXTENSION, url.toString())
  }

  /**
   * PACKAGE_RESOLVE(packageSpecifier, parentURL).
   */
  private fun packageResolve(packageSpecifier: String, parentURL: URI, env: Env): URI {
    // 1. Let packageName be undefined.
    val packageName: String
    // 2. If packageSpecifier is an empty string, then
    if (packageSpecifier.isEmpty()) {
      // Throw an Invalid Module Specifier error.
      throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier)
    }
    // 3. Note: we ignore Node.js builtin module names.
    val packageSpecifierSeparator = packageSpecifier.indexOf('/')
    // 4. If packageSpecifier does not start with "@", then
    if (packageSpecifier[0] != '@') {
      // Set packageName to the substring of packageSpecifier until the first "/"
      packageName = if (packageSpecifierSeparator != -1) {
        packageSpecifier.substring(0, packageSpecifierSeparator)
      } else {
        // or the end of the string.
        packageSpecifier
      }
    } else {
      // 5. Otherwise, if packageSpecifier does not contain a "/" separator, then
      if (packageSpecifierSeparator == -1) {
        // Throw an Invalid Module Specifier error.
        throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier)
      }
      // Set packageName to the substring of packageSpecifier until the second "/" separator
      val secondSeparator = packageSpecifier.indexOf('/', packageSpecifierSeparator + 1)
      packageName = if (secondSeparator != -1) {
        packageSpecifier.substring(0, secondSeparator)
      } else {
        // or the end of the string.
        packageSpecifier
      }
    }
    // 6. If packageName starts with "." or contains "\" or "%", then
    if (packageName[0] == '.' || packageName.indexOf('\\') >= 0 || packageName.indexOf('%') >= 0) {
      // Throw an Invalid Module Specifier error.
      throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier)
    }
    // 7. Let packageSubpath be "." concatenated with the substring of packageSpecifier from the
    // position at the length of packageName.
    val packageSpecifierSub = packageSpecifier.substring(packageName.length)
    val packageSubpath = DOT + packageSpecifierSub
    // 8. If packageSubpath ends in "/", then
    if (packageSubpath.endsWith(SLASH)) {
      // Throw an Invalid Module Specifier error.
      throw fail(INVALID_MODULE_SPECIFIER, packageSpecifier)
    }
    // 9. Let selfUrl be the result of PACKAGE_SELF_RESOLVE(packageName, packageSubpath,
    // parentURL).
    val selfUrl = packageSelfResolve(packageName, parentURL, env)
    // 10. If selfUrl is not undefined, return selfUrl.
    if (selfUrl != null) {
      return selfUrl
    }
    var currentParentUrl = env.getPublicTruffleFile(parentURL)
    // 11. While parentURL is not the file system root,
    while (currentParentUrl != null && !isRoot(currentParentUrl)) {
      // 11.1 Let packageURL be the URL resolution of "node_modules/" concatenated with
      // packageSpecifier, relative to parentURL.
      val packageUrl = getPackageUrl(packageName, currentParentUrl)
      // 11.2 Set parentURL to the parent folder URL of parentURL.
      currentParentUrl = currentParentUrl.parent
      // 11.3 If the folder at packageURL does not exist, then
      val maybeFolder = if (packageUrl != null) env.getPublicTruffleFile(packageUrl) else null
      if (maybeFolder == null || !maybeFolder.exists() || !maybeFolder.isDirectory()) {
        continue
      }
      // 11.4 Let pjson be the result of READ_PACKAGE_JSON(packageURL).
      val pjson = readPackageJson(packageUrl, env)
      // 11.5 If pjson is not null and pjson.exports is not null or undefined, then
      if (pjson != null && pjson.hasExportsProperty()) {
        throw fail(UNSUPPORTED_PACKAGE_EXPORTS, packageSpecifier)
      } else if (packageSubpath == DOT) {
        // 11.6 Otherwise, if packageSubpath is equal to ".", then
        // 11.6.1 If pjson.main is a string, then return the URL resolution of main in
        // packageURL.
        if (pjson != null && pjson.hasMainProperty()) {
          val main = pjson.mainProperty
          return packageUrl!!.resolve(main.toString())
        } else {
          // For backwards compatibility: return null and try loading as a legacy CJS.
          // https://github.com/oracle/graaljs/blob/master/graal-nodejs/lib/internal/modules/esm/resolve.js#L918
          return TryCommonJS
        }
      }
      // 7. Otherwise, Return the URL resolution of packageSubpath in packageURL.
      return packageUrl!!.resolve(packageSubpath)
    }
    // 12. Will Throw a Module Not Found error.
    return TryCustomESM
  }

  /**
   * PACKAGE_SELF_RESOLVE(packageName, packageSubpath, parentURL).
   */
  private fun packageSelfResolve(packageName: String, parentURL: URI, env: Env): URI? {
    // 1. Let packageURL be the result of LOOKUP_PACKAGE_SCOPE(parentURL).
    val packageUrl = lookupPackageScope(parentURL, env) ?: return null
    // 2. If packageURL is null, then Return undefined.
    // 3. Let pjson be the result of READ_PACKAGE_JSON(packageURL).
    val pjson = readPackageJson(packageUrl, env)
    // 4. If pjson is null or if pjson.exports is null or undefined, Return undefined.
    if (pjson == null || !pjson.hasExportsProperty()) {
      return null
    }
    // 5. If pjson.name is equal to packageName, then
    if (pjson.namePropertyEquals(packageName)) {
      throw failMessage(UNSUPPORTED_PACKAGE_EXPORTS)
    }
    // 6. Otherwise, return undefined.
    return null
  }

  /**
   * LOOKUP_PACKAGE_SCOPE(url).
   */
  private fun lookupPackageScope(url: URI?, env: Env): URI? {
    // 1. Let scopeURL be url
    var scopeUrl = url
    // 2. While scopeURL is not the file system root,
    while (scopeUrl != null) {
      // 2.1. Set scopeURL to the parent URL of scopeURL.
      scopeUrl = getParentUrl(scopeUrl, env)
      if (scopeUrl == null) {
        break
      }
      // 2.2 If scopeURL ends in a "node_modules" path segment, return null.
      if (scopeUrl.toString().endsWith(CommonJSResolution.NODE_MODULES)) {
        return null
      }
      // 2.3 Let pjsonURL be the resolution of "package.json" within scopeURL.
      // 2.4 if the file at pjsonURL exists, then Return scopeURL
      if (readPackageJson(scopeUrl, env) != null) {
        return scopeUrl
      }
    }
    // 3. Return null.
    return null
  }

  //
  // ##### Utils
  //
  private enum class Format {
    CommonJS,
    ESM
  }

  private class PackageJson(jsonObj: JSDynamicObject?) {
    private val jsonObj: JSDynamicObject

    init {
      checkNotNull(jsonObj)
      assert(JSObject.isJSObject(jsonObj))
      this.jsonObj = jsonObj
    }

    fun hasTypeModule(): Boolean {
      if (hasNonNullProperty(jsonObj, Strings.TYPE)) {
        val nameValue = JSObject.get(jsonObj, Strings.TYPE)
        if (nameValue is TruffleString) {
          return Strings.equals(Strings.MODULE, nameValue)
        }
      }
      return false
    }

    fun hasExportsProperty(): Boolean {
      return hasNonNullProperty(jsonObj, Strings.EXPORTS_PROPERTY_NAME)
    }

    fun hasMainProperty(): Boolean {
      if (JSObject.hasProperty(jsonObj, Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME)) {
        val value = JSObject.get(jsonObj, Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME)
        return Strings.isTString(value)
      }
      return false
    }

    val mainProperty: TruffleString
      get() {
        assert(hasMainProperty())
        val value = JSObject.get(jsonObj, Strings.PACKAGE_JSON_MAIN_PROPERTY_NAME)
        return value as TruffleString
      }

    fun namePropertyEquals(name: String?): Boolean {
      val packageName = Strings.fromJavaString(name)
      if (hasNonNullProperty(jsonObj, Strings.NAME)) {
        val nameValue = JSObject.get(jsonObj, Strings.NAME)
        if (nameValue is TruffleString) {
          return Strings.equals(packageName, nameValue)
        }
      }
      return false
    }

    companion object {
      private fun hasNonNullProperty(`object`: JSDynamicObject, keyName: TruffleString): Boolean {
        if (JSObject.hasProperty(`object`, keyName)) {
          val value = JSObject.get(`object`, keyName)
          return value !== Null.instance && value !== Undefined.instance
        }
        return false
      }
    }
  }

  private fun readPackageJson(packageUrl: URI?, env: Env): PackageJson? {
    val pjsonUrl = packageUrl!!.resolve(CommonJSResolution.PACKAGE_JSON)
    if (!fileExists(pjsonUrl, env)) {
      return null
    }
    val jsonObj = CommonJSResolution.loadJsonObject(env.getPublicTruffleFile(pjsonUrl), realm)
    if (!JSDynamicObject.isJSDynamicObject(jsonObj)) {
      throw failMessage(INVALID_PACKAGE_CONFIGURATION)
    }
    return PackageJson(jsonObj)
  }

  private fun getFullPath(referencingModule: ScriptOrModule?): TruffleFile {
    var refPath = referencingModule?.source?.path
    if (refPath == null) {
      refPath = realm.contextOptions.requireCwd
    }
    return realm.env.getPublicTruffleFile(refPath)
  }

  companion object {
    private val TryCommonJS: URI = URI.create("custom:///try-common-js-token")
    private val TryCustomESM: URI = URI.create("custom:///try-custom-esm-token")

    private const val MODULE_NOT_FOUND = "Module not found: '"
    private const val UNSUPPORTED_JSON = "JSON packages not supported."
    private const val FAILED_BUILTIN = "Failed to load built-in ES module: '"
    private const val INVALID_MODULE_SPECIFIER = "Invalid module specifier: '"
    private const val UNSUPPORTED_FILE_EXTENSION = "Unsupported file extension: '"
    private const val UNSUPPORTED_PACKAGE_EXPORTS = "Unsupported package exports: '"
    private const val UNSUPPORTED_PACKAGE_IMPORTS = "Unsupported package imports: '"
    private const val UNSUPPORTED_DIRECTORY_IMPORT = "Unsupported directory import: '"
    private const val INVALID_PACKAGE_CONFIGURATION = "Invalid package configuration: '"

    private fun isRoot(file: TruffleFile): Boolean {
      if (file.isDirectory() && file.isAbsolute) {
        return file.parent == null
      }
      return false
    }

    private fun fileExists(url: URI?, env: Env): Boolean {
      return CommonJSResolution.fileExists(env.getPublicTruffleFile(url))
    }

    private fun isFileURI(maybe: URI?): Boolean {
      return maybe != null && maybe.scheme == CommonJSResolution.FILE
    }

    private fun getPackageUrl(packageSpecifier: String, parentURL: TruffleFile): URI? {
      try {
        val combined = URI("./" + CommonJSResolution.NODE_MODULES + "/" + packageSpecifier)
        val resolved = parentURL.resolve(combined.toString())
        return resolved.toUri()
      } catch (e: URISyntaxException) {
        // will handle null return
      }
      return null
    }

    private fun getParentUrl(scopeUrl: URI, env: Env): URI? {
      val asFile = env.getPublicTruffleFile(scopeUrl)
      if (asFile.parent != null) {
        return asFile.parent.toUri()
      }
      return null
    }

    private fun getAssociatedDefaultFormat(resolved: URI): Format {
      checkNotNull(resolved.path)
      if (resolved.path.endsWith(CommonJSResolution.MJS_EXT)) {
        return ESM
      }
      // By default, try loading as CJS if not .mjs
      return CommonJS
    }

    private fun isDirectory(resolved: URI, env: Env): Boolean {
      return env.getPublicTruffleFile(resolved).isDirectory()
    }

    private fun resolveRelativeToParent(specifier: String, parentURL: URI): URI {
      return parentURL.resolve(specifier)
    }

    @TruffleBoundary private fun failMessage(message: String): JSException {
      return JSException.create(TypeError, message)
    }

    @TruffleBoundary private fun fail(errorType: String, moduleIdentifier: String): JSException {
      return failMessage(errorType + moduleIdentifier + Strings.SINGLE_QUOTE)
    }

    private fun isRelativePathFileName(moduleIdentifier: String): Boolean {
      return moduleIdentifier.startsWith(DOT_SLASH) || moduleIdentifier.startsWith(DOT_DOT_SLASH)
    }
  }
}
