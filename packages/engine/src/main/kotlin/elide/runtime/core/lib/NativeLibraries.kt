/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.core.lib

/**
 * # Native Libraries
 *
 * Utility for loading native libraries within the context of either a JVM or an SVM (native) application; SVM requires
 * certain steps to be performed, or to be skipped, based on dynamic or static linkage of libraries.
 *
 * Static libraries are built-in to the native image ahead of time, and their load calls should be skipped at runtime.
 * Dynamic libraries are loaded via normal JVM means.
 */
public object NativeLibraries {
  /**
   * Resolve a library by name.
   *
   * This method attempts to load the library at the provided [name], returning a boolean value indicating whether the
   * library was loaded successfully.
   *
   * The name of the library is expected to be in "naked" form; i.e., without system prefixing or extensions.
   *
   * For example, to load the library `sqlitejdbc`, the name should be provided as `sqlitejdbc`, and the system will
   * load `libsqlitejbc.<so|dylib|dll>` as appropriate.
   *
   * @param name The name of the library to load.
   * @return A boolean value indicating whether the library was loaded successfully.
   */
  public fun resolve(name: String, callback: ((Boolean) -> Unit)? = null): Boolean {
    return runCatching { System.loadLibrary(name) }.isSuccess.also {
      callback?.invoke(it)
    }
  }

  /**
   * Load a library by name.
   *
   * This call corresponds with [System.loadLibrary] but with adaptations for SVM compatibility; see JDK docs for
   * detailed steps performed during library loading.
   *
   * Libraries loaded in this manner are expected to be found on the `java.library.path`.
   *
   * @param name The name of the library to load.
   * @return A boolean value indicating whether the library was loaded successfully.
   */
  public fun loadLibrary(name: String): Boolean = resolve(name)

  /**
   * Load a library by path and name.
   *
   * This call corresponds with [System.load] but with adaptations for SVM compatibility; see JDK docs for detailed
   * steps performed during library loading.
   *
   * Libraries loaded in this manner are expected to be found at the provided path.
   *
   * @param path The path to the library.
   * @param name The name of the library to load.
   */
  public fun loadLibrary(path: String, name: String): Boolean = runCatching { System.load(path + name) }.isSuccess
}
