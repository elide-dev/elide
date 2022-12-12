package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.runtime.intrinsics.js.FetchURL
import java.net.URL

/** Implements an intrinsic for the `URL` global defined by the Fetch API. */
@Suppress("unused", "UNUSED_PARAMETER")
internal class FetchURLIntrinsic private constructor (target: URL) : FetchURL {
  /** Typed constructor interfaces for the `URL` global. */
  internal object URLConstructors {

  }

  /** @inheritDoc */
  override fun toString(): String {
    TODO("Not yet implemented")
  }
}
