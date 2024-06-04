package elide.tool.code

import java.io.InputStream
import java.nio.channels.SeekableByteChannel
import java.nio.file.Path

sealed interface CodeIdentity : Comparable<CodeIdentity> {
  override fun compareTo(other: CodeIdentity): Int {
    return when {
      this == other -> 0
      this.hashCode() < other.hashCode() -> -1
      else -> 1
    }
  }
}

@JvmInline value class Filename(val name: String) : CodeIdentity {
  override fun toString(): String {
    return "Filename($name)"
  }
}

@JvmInline value class Identity(val value: Any) : CodeIdentity {
  override fun toString(): String {
    return "Identity($value)"
  }
}

sealed interface CodeSource {
  val identity: CodeIdentity

  class File(val path: Path) : CodeSource {
    override val identity: CodeIdentity = Filename(path.fileName.toString())
  }

  class Literal(val code: String) : CodeSource {
    override val identity: CodeIdentity = Identity(code.hashCode())
  }

  class Stream(val id: Long, val stream: InputStream) : CodeSource {
    override val identity: CodeIdentity = Identity(id)
  }

  class Channel(val id: Long, val channel: SeekableByteChannel) : CodeSource {
    override val identity: CodeIdentity = Identity(id)
  }
}
