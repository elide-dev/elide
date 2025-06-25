package elide.runtime.exec

import elide.tooling.Tool

public fun Tool.Result.asExecResult(): elide.exec.Result {
  return when (this) {
    is Tool.Result.Success -> elide.exec.Result.Nothing
    is Tool.Result.UnspecifiedFailure -> elide.exec.Result.UnspecifiedFailure
  }
}
