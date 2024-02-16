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

package elide.runtime.gvm.internals.python

import java.util.*
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.ExecutableScript.ScriptType
import elide.runtime.gvm.InvocationMode
import elide.runtime.gvm.internals.AbstractGVMScript
import elide.runtime.gvm.internals.GraalVMGuest.PYTHON

/**
 * TBD.
 */
internal class PythonExecutableScript(
  source: ExecutableScript.ScriptSource,
  spec: String,
) : AbstractGVMScript(
  PYTHON,
  source,
  spec
) {
  override fun invocation(): EnumSet<InvocationMode> {
    TODO("Not yet implemented")
  }

  override fun type(): ScriptType {
    TODO("Not yet implemented")
  }
}
