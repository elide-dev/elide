package elide.runtime.plugins.java.shell

import org.graalvm.polyglot.PolyglotException
import elide.runtime.core.DelicateElideApi

@DelicateElideApi internal fun interface GuestExceptionMapper {
  fun map(guestException: PolyglotException): Exception
}