package elide.runtime.plugins.java.shell

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

@DelicateElideApi @JvmInline internal value class GuestExceptionWrapper private constructor(val value: PolyglotValue) {
  fun message(): String? {
    return value.invokeMember("getMessage").asStringOrNull()
  }

  fun cause(): String? {
    return value.invokeMember("causeExceptionClass").asStringOrNull()
  }

  fun stackTrace(): Array<StackTraceElement> {
    // TODO(@darvld): implement stack trace mapping
    return emptyArray()
  }

  infix fun isMetaInstanceOf(guestClass: PolyglotValue): Boolean {
    return guestClass.isMetaInstance(value)
  }

  internal companion object {
    infix fun of(value: PolyglotValue): GuestExceptionWrapper {
      check(value.isException)
      return GuestExceptionWrapper(value)
    }
  }
}