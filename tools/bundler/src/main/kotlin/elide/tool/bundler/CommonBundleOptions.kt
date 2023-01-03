package elide.tool.bundler

import picocli.CommandLine
import java.io.File

/**
 * TBD.
 */
internal class CommonBundleOptions {
  /** Bundle file we are working with, as applicable. */
  @set:CommandLine.Option(
    names = ["-f", "--bundle"],
    paramLabel = "FILE",
    description = ["Specifies the bundle file we should work with"],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var file: File? = null

  /** Explicit flag to consume from `stdin`. */
  @set:CommandLine.Option(
    names = ["--stdin"],
    description = ["Indicates that the bundler should wait for data from standard-in"],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var stdin: Boolean = false
}
