package elide.runtime.gvm.internals.vfs

import java.io.IOException

/** Internal base class for I/O exceptions raised from guest operations. */
public abstract class GuestIOException constructor(
  message: String? = null,
  cause: Throwable? = null,
) : IOException(message, cause)
