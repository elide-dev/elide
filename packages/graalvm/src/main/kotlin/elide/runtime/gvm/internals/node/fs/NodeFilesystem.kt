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
package elide.runtime.gvm.internals.node.fs

import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.node.FilesystemAPI
import elide.runtime.intrinsics.js.node.FilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.fs.FileHandle
import elide.runtime.intrinsics.js.node.fs.ReadFileCallback
import elide.runtime.intrinsics.js.node.fs.ReadFileOptions
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer

// Installs the Node `fs` and `fs/promises` modules into the intrinsic bindings.
@Intrinsic internal class NodeFilesystemModule : AbstractNodeBuiltinModule() {
  override fun install(bindings: MutableIntrinsicBindings) {
//    bindings[NodeFilesystem.SYMBOL_STD.asJsSymbol()] = NodeFilesystem.createStd(filesystem)
//    bindings[NodeFilesystem.SYMBOL_PROMISES.asJsSymbol()] = NodeFilesystem.createPromises(filesystem)
  }
}

// Implements the Node built-in filesystem modules.
internal object NodeFilesystem {
  internal const val SYMBOL_STD: String = "__Elide_node_fs__"
  internal const val SYMBOL_PROMISES: String = "__Elide_node_fs_promises__"

  /** @return Host-only implementation of the `fs` module. */
  fun createStd(): FilesystemAPI = TODO("host-only node fs not yet implemented")

  /** @return Host-only implementation of the `fs/promises` module. */
  fun createPromises(): FilesystemPromiseAPI = TODO("host-only node fs not yet implemented")

  /** @return Instance of the `fs` module. */
  fun createStd(filesystem: GuestVFS): FilesystemAPI = NodeFilesystemProxy(filesystem)

  /** @return Instance of the `fs/promises` module. */
  fun createPromises(filesystem: GuestVFS): FilesystemPromiseAPI = NodeFilesystemPromiseProxy(filesystem)
}

// Implements common baseline functionality for the Node filesystem modules.
private abstract class FilesystemBaseline (protected val fs: GuestVFS)

// Implements the Node `fs` module.
private class NodeFilesystemProxy (fs: GuestVFS) : FilesystemAPI, FilesystemBaseline(fs) {
  override fun readFile(path: String, options: ReadFileOptions, callback: ReadFileCallback) {
    TODO("Not yet implemented: fs.readFile (Node API)")
  }

  override fun readFile(url: URL, options: ReadFileOptions, callback: ReadFileCallback) {
    TODO("Not yet implemented: fs.readFile (Node API)")
  }

  override fun readFile(handle: FileHandle, options: ReadFileOptions, callback: ReadFileCallback) {
    TODO("Not yet implemented: fs.readFile (Node API)")
  }

  override fun readFile(buffer: Buffer, options: ReadFileOptions, callback: ReadFileCallback) {
    TODO("Not yet implemented: fs.readFile (Node API)")
  }

  override fun readFileSync(path: String, options: ReadFileOptions): StringOrBuffer {
    TODO("Not yet implemented")
  }
}

// Implements the Node `fs/promises` module.
private class NodeFilesystemPromiseProxy (fs: GuestVFS) : FilesystemPromiseAPI, FilesystemBaseline(fs) {
  override fun readFile(path: String, options: ReadFileOptions): JsPromise<StringOrBuffer> {
    TODO("Not yet implemented: fs/promises.readFile (Node API)")
  }

  override fun readFile(path: URL, options: ReadFileOptions): JsPromise<StringOrBuffer> {
    TODO("Not yet implemented: fs/promises.readFile (Node API)")
  }

  override fun readFile(handle: FileHandle, options: ReadFileOptions): JsPromise<StringOrBuffer> {
    TODO("Not yet implemented: fs/promises.readFile (Node API)")
  }

  override fun readFile(buffer: Buffer, options: ReadFileOptions): JsPromise<StringOrBuffer> {
    TODO("Not yet implemented: fs/promises.readFile (Node API)")
  }
}
