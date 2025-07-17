package dev.elide.intellij

import com.intellij.openapi.util.io.toCanonicalPath
import java.nio.file.Path

class InvalidElideHomeException(path: String) : Exception() {
  constructor(path: Path) : this(path.toCanonicalPath())
  override val message: String = "Invalid Elide distribution specified: $path"
}
