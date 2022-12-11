package elide.runtime.gvm.intrinsics

import elide.runtime.gvm.GuestLanguage

/**
 * # Guest Intrinsic
 *
 * Applied to all intrinsic classes which are implemented for a guest language, in addition to various annotations which
 * designate the use context of a given implementation.
 */
internal interface GuestIntrinsic {
  /**
   * Indicate the language which this intrinsic is intended to be used with.
   *
   * @return Guest language bound to this intrinsic.
   */
  fun language(): GuestLanguage

  /**
   * Indicate whether this intrinsic is intended to be used with a given guest [language].
   *
   * @param language Language to check.
   * @return `true` if this intrinsic is intended to be used with the given language, `false` otherwise.
   */
  fun supports(language: GuestLanguage): Boolean {
    return language().symbol == language.symbol
  }
}
