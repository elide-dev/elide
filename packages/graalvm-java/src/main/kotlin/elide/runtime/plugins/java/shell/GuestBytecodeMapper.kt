package elide.runtime.plugins.java.shell

import jdk.jshell.spi.ExecutionControl.ClassBytecodes
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

@DelicateElideApi internal fun interface GuestBytecodeMapper {
  fun map(bytecodes: Array<out ClassBytecodes>): PolyglotValue
}