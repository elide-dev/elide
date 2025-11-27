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
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.vm.annotations.Polyglot

/**
 * Implements the FormData Web API interface.
 *
 * FormData provides a way to construct a set of key/value pairs representing form fields
 * and their values, which can be sent using fetch() or XMLHttpRequest.
 */
public class FormData : ProxyObject, ProxyIterable {
  private val entries: MutableMap<String, MutableList<Any>> = mutableMapOf()

  /** Appends a new value onto an existing key, or adds the key if it doesn't exist. */
  @Polyglot
  public fun append(name: String, value: Any) {
    entries.getOrPut(name) { mutableListOf() }.add(value)
  }

  /** Deletes a key and all its values. */
  @Polyglot
  public fun delete(name: String) {
    entries.remove(name)
  }

  /** Returns an iterator of all key/value pairs. */
  @Polyglot
  public fun entries(): Iterator<Array<Any>> = iterator {
    for ((key, values) in entries) {
      for (value in values) {
        yield(arrayOf(key, value))
      }
    }
  }

  /** Returns the first value associated with a given key. */
  @Polyglot
  public fun get(name: String): Any? = entries[name]?.firstOrNull()

  /** Returns all values associated with a given key. */
  @Polyglot
  public fun getAll(name: String): List<Any> = entries[name] ?: emptyList()

  /** Returns whether a key exists. */
  @Polyglot
  public fun has(name: String): Boolean = entries.containsKey(name)

  /** Returns an iterator of all keys. */
  @Polyglot
  public fun keys(): Iterator<String> = entries.keys.iterator()

  /** Sets a new value for an existing key, or adds the key/value if it doesn't exist. */
  @Polyglot
  public fun set(name: String, value: Any) {
    entries[name] = mutableListOf(value)
  }

  /** Returns an iterator of all values. */
  @Polyglot
  public fun values(): Iterator<Any> = iterator {
    for (values in entries.values) {
      for (value in values) {
        yield(value)
      }
    }
  }

  // ProxyObject implementation
  override fun getMemberKeys(): Any = arrayOf("append", "delete", "entries", "get", "getAll", "has", "keys", "set", "values")

  override fun hasMember(key: String?): Boolean = key in arrayOf("append", "delete", "entries", "get", "getAll", "has", "keys", "set", "values")

  override fun getMember(key: String?): Any? = when (key) {
    "append" -> { name: String, value: Any -> append(name, value) }
    "delete" -> { name: String -> delete(name) }
    "entries" -> entries()
    "get" -> { name: String -> get(name) }
    "getAll" -> { name: String -> getAll(name) }
    "has" -> { name: String -> has(name) }
    "keys" -> keys()
    "set" -> { name: String, value: Any -> set(name, value) }
    "values" -> values()
    else -> null
  }

  override fun putMember(key: String?, value: org.graalvm.polyglot.Value?) {
    // FormData is not directly mutable via property access
  }

  // ProxyIterable implementation - iterates over entries
  override fun getIterator(): Any = entries()

  public companion object {
    /**
     * Parse URL-encoded form data (application/x-www-form-urlencoded).
     */
    @JvmStatic
    public fun parseUrlEncoded(body: String): FormData {
      val formData = FormData()
      if (body.isBlank()) return formData

      for (pair in body.split("&")) {
        val parts = pair.split("=", limit = 2)
        val key = java.net.URLDecoder.decode(parts[0], Charsets.UTF_8)
        val value = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], Charsets.UTF_8) else ""
        formData.append(key, value)
      }
      return formData
    }

    /**
     * Parse multipart/form-data content.
     *
     * @param body The raw body bytes
     * @param boundary The boundary string from Content-Type header
     * @return Parsed FormData with fields and files
     */
    @JvmStatic
    public fun parseMultipart(body: ByteArray, boundary: String): FormData {
      val formData = FormData()
      val boundaryBytes = "--$boundary".toByteArray(Charsets.UTF_8)
      val endBoundaryBytes = "--$boundary--".toByteArray(Charsets.UTF_8)

      // Split body by boundary
      val parts = splitByBoundary(body, boundaryBytes)

      for (part in parts) {
        if (part.isEmpty() || part.contentEquals(endBoundaryBytes)) continue

        // Parse headers and content from each part
        val parsed = parseMultipartPart(part)
        if (parsed != null) {
          formData.append(parsed.first, parsed.second)
        }
      }

      return formData
    }

    /**
     * Extract boundary from Content-Type header.
     * Example: "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW"
     */
    @JvmStatic
    public fun extractBoundary(contentType: String): String? {
      val boundaryMatch = Regex("""boundary=([^\s;]+)""").find(contentType)
      return boundaryMatch?.groupValues?.get(1)?.trim('"')
    }

    private fun splitByBoundary(body: ByteArray, boundary: ByteArray): List<ByteArray> {
      val parts = mutableListOf<ByteArray>()
      var start = 0

      while (start < body.size) {
        val boundaryIndex = indexOf(body, boundary, start)
        if (boundaryIndex == -1) {
          // No more boundaries, add remaining content
          if (start < body.size) {
            parts.add(body.copyOfRange(start, body.size))
          }
          break
        }

        if (boundaryIndex > start) {
          // Add content before boundary (skip leading CRLF)
          var contentStart = start
          if (body.size > contentStart + 1 && body[contentStart] == '\r'.code.toByte() && body[contentStart + 1] == '\n'.code.toByte()) {
            contentStart += 2
          }
          if (boundaryIndex > contentStart) {
            // Remove trailing CRLF before boundary
            var contentEnd = boundaryIndex
            if (contentEnd >= 2 && body[contentEnd - 2] == '\r'.code.toByte() && body[contentEnd - 1] == '\n'.code.toByte()) {
              contentEnd -= 2
            }
            if (contentEnd > contentStart) {
              parts.add(body.copyOfRange(contentStart, contentEnd))
            }
          }
        }

        start = boundaryIndex + boundary.size
      }

      return parts
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, start: Int = 0): Int {
      outer@ for (i in start..(haystack.size - needle.size)) {
        for (j in needle.indices) {
          if (haystack[i + j] != needle[j]) continue@outer
        }
        return i
      }
      return -1
    }

    private fun parseMultipartPart(part: ByteArray): Pair<String, Any>? {
      // Find the blank line separating headers from content (CRLFCRLF)
      val headerEndIndex = findHeaderEnd(part)
      if (headerEndIndex == -1) return null

      val headerBytes = part.copyOfRange(0, headerEndIndex)
      val contentBytes = part.copyOfRange(headerEndIndex + 4, part.size) // +4 for CRLFCRLF

      val headers = String(headerBytes, Charsets.UTF_8)
      val headerMap = parsePartHeaders(headers)

      val contentDisposition = headerMap["content-disposition"] ?: return null
      val name = extractDispositionParam(contentDisposition, "name") ?: return null
      val filename = extractDispositionParam(contentDisposition, "filename")
      val contentType = headerMap["content-type"]

      return if (filename != null) {
        // This is a file field
        name to FormDataFile(
          bytes = contentBytes,
          name = filename,
          type = contentType,
          lastModified = System.currentTimeMillis()
        )
      } else {
        // This is a text field
        name to String(contentBytes, Charsets.UTF_8)
      }
    }

    private fun findHeaderEnd(data: ByteArray): Int {
      for (i in 0..(data.size - 4)) {
        if (data[i] == '\r'.code.toByte() &&
            data[i + 1] == '\n'.code.toByte() &&
            data[i + 2] == '\r'.code.toByte() &&
            data[i + 3] == '\n'.code.toByte()) {
          return i
        }
      }
      return -1
    }

    private fun parsePartHeaders(headers: String): Map<String, String> {
      return headers.split("\r\n")
        .filter { it.contains(":") }
        .associate { line ->
          val colonIndex = line.indexOf(':')
          val key = line.substring(0, colonIndex).trim().lowercase()
          val value = line.substring(colonIndex + 1).trim()
          key to value
        }
    }

    private fun extractDispositionParam(disposition: String, param: String): String? {
      val regex = Regex("""$param="([^"]+)"""")
      return regex.find(disposition)?.groupValues?.get(1)
    }
  }
}

/**
 * Represents a file from multipart form data.
 * Implements the File interface from the Web API.
 */
public class FormDataFile(
  private val bytes: ByteArray,
  /** The name of the file. */
  @get:Polyglot public val name: String,
  @get:Polyglot public override val type: String?,
  /** An epoch timestamp indicating the last modification to this file. */
  @get:Polyglot public val lastModified: Long
) : Blob, ProxyObject {

  @get:Polyglot
  public override val size: Int = bytes.size

  @Polyglot
  public override fun arrayBuffer(): JsPromise<org.graalvm.polyglot.Value> {
    return JsPromise.resolved(org.graalvm.polyglot.Value.asValue(java.nio.ByteBuffer.wrap(bytes)))
  }

  @Polyglot
  public override fun slice(start: Int?, end: Int?, type: String?): Blob {
    val slicedBytes = bytes.copyOfRange(start ?: 0, end ?: bytes.size)
    return FormDataFile(slicedBytes, name, type ?: this.type, lastModified)
  }

  @Polyglot
  public override fun text(): JsPromise<String> {
    return JsPromise.resolved(bytes.toString(Charsets.UTF_8))
  }

  @Polyglot
  public override fun stream(): ReadableStream {
    return ReadableStream.wrap(bytes)
  }

  // ProxyObject implementation
  override fun getMemberKeys(): Any = arrayOf("name", "size", "type", "lastModified", "arrayBuffer", "slice", "text", "stream")

  override fun hasMember(key: String?): Boolean = key in arrayOf("name", "size", "type", "lastModified", "arrayBuffer", "slice", "text", "stream")

  override fun getMember(key: String?): Any? = when (key) {
    "name" -> name
    "size" -> size
    "type" -> type
    "lastModified" -> lastModified
    else -> null
  }

  override fun putMember(key: String?, value: org.graalvm.polyglot.Value?) {
    // File is immutable
  }
}
