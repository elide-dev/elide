package elide.runtime.js

import kotlin.js.Promise
import js.import.import as jsImport

/**
 *
 */
public external fun require(module: String): dynamic

/**
 *
 */
public fun <T : Any> import(path: String): Promise<T> {
  return jsImport(path)
}
