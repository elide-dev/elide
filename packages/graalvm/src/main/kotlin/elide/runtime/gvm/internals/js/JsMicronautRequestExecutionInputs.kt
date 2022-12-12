package elide.runtime.gvm.internals.js

import elide.runtime.gvm.MicronautRequestExecutionInputs
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchRequest
import elide.runtime.intrinsics.js.ReadableStream

/** TBD. */
internal class JsMicronautRequestExecutionInputs : MicronautRequestExecutionInputs(), FetchRequest {
  override val body: ReadableStream
    get() = TODO("Not yet implemented")
  override val bodyUsed: Boolean
    get() = TODO("Not yet implemented")
  override val destination: String
    get() = TODO("Not yet implemented")
  override val headers: FetchHeaders
    get() = TODO("Not yet implemented")
  override val url: String
    get() = TODO("Not yet implemented")
}
