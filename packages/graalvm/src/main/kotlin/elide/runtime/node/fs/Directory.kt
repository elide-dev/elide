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
package elide.runtime.node.fs

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyIterator
import java.io.File
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.atomicfu.atomic
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.fs.Dir
import elide.runtime.intrinsics.js.node.fs.Dirent
import elide.vm.annotations.Polyglot

private const val DIR_PROP_PATH = "path"
private const val DIR_METHOD_CLOSE = "close"
private const val DIR_METHOD_READ = "read"
private const val DIR_METHOD_READ_SYNC = "readSync"

private val dirPropsAndMethods = arrayOf(
  DIR_PROP_PATH,
  DIR_METHOD_CLOSE,
  DIR_METHOD_READ,
  DIR_METHOD_READ_SYNC,
)

/**
 * ## Node Filesystem: Directory
 *
 * Implements a wrapping class/object for a directory instance; can iterate over the entries for the directory, as well
 * as provide basic information about entries.
 */
public class Directory private constructor (
  file: File,
  private val originalPath: String,
  private val walker: DirectoryStream<Path>,
): Dir {
  public companion object Factory: ProxyInstantiable {
    /**
     * Create a new [Directory] handle from the provided [file] and directory [walker].
     *
     * @param file File to wrap.
     * @param walker Directory walker.
     * @return Directory instance.
     */
    @JvmStatic public fun wrap(file: File, walker: DirectoryStream<Path>): Directory {
      return Directory(file, file.path, walker)
    }

    /**
     * Create a new [Directory] handle from the provided [file].
     *
     * @param file File to wrap.
     * @param path Path to the file.
     * @return Directory instance.
     */
    @JvmStatic public fun of(file: File, path: Path): Directory {
      return wrap(file, Files.newDirectoryStream(path))
    }

    override fun newInstance(vararg arguments: Value?): Any? {
      TODO("Not yet implemented")
    }
  }

  private val handle = atomic<File?>(file)
  private val closed = atomic(false)
  private val closeCallback = atomic<(() -> Unit)?>(null)
  private val activeIter = atomic<Iterator<Path>>(walker.iterator())

  private inline fun <R> withNotClosed(cbk: () -> R): R {
    require(!closed.value) { "Cannot operate on `Dir`: closed" }
    return cbk.invoke()
  }

  private fun iterateNext(): Path? {
    val iter = activeIter.value
    return if (iter.hasNext()) iter.next() else null
  }

  override fun getMemberKeys(): Array<String> = dirPropsAndMethods

  override fun getIterator(): ProxyIterator = walker.iterator().let { iter ->
    object: ProxyIterator {
      override fun hasNext(): Boolean = withNotClosed {
        iter.hasNext()
      }

      override fun getNext(): Dirent? = withNotClosed {
        iter.next()?.let { DirectoryEntry.forPath(it) }
      }
    }
  }

  override fun getMember(key: String): Any? = when (key) {
    DIR_PROP_PATH -> path

    DIR_METHOD_CLOSE -> ProxyExecutable {
      when (it.size) {
        0 -> close()
        else -> close(it.first())
      }
    }

    DIR_METHOD_READ -> ProxyExecutable {
      val first = it.firstOrNull() ?: throw JsError.typeError("Must provide a callback to `read()`")
      read(first)
    }

    DIR_METHOD_READ_SYNC -> ProxyExecutable {
      readSync()
    }

    else -> null
  }

  @get:Polyglot override val path: String get() = originalPath

  @Polyglot override fun close() {
    closed.value = true
    handle.value = null
    closeCallback.value?.invoke()
    closeCallback.value = null
  }

  override fun close(callback: () -> Unit): Unit = withNotClosed {
    closeCallback.value = callback
    close()
  }

  @Polyglot override fun close(callback: Value): Unit = withNotClosed {
    close {
      callback.executeVoid()
    }
  }

  @Polyglot override fun closeSync(): Unit = withNotClosed {
    close()
  }

  @Polyglot override fun read(callback: Value): Unit = withNotClosed {
    require(callback.canExecute()) { "Callback passed to `read()` must be executable" }

    when (val next = iterateNext()) {
      null -> callback.executeVoid()
      else -> callback.execute(DirectoryEntry.forPath(next))
    }
  }

  @Polyglot override fun readSync(): Dirent? = withNotClosed {
    iterateNext()?.let {
      DirectoryEntry.forPath(it)
    }
  }
}
