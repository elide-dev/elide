package elide.runtime.gvm

import elide.runtime.gvm.intrinsics.GuestIntrinsic

/**
 * # Intrinsics Resolver
 *
 * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; typically,
 * intrinsics are resolved at build-time and defined statically within an application or image.
 */
internal interface IntrinsicsResolver {
  /**
   * Resolves a set of intrinsic implementation objects which should be provided to a given guest VM; intrinsics are
   * typically resolved at build time.
   *
   * @param language Language to resolve intrinsics for.
   * @return A set of intrinsic implementation objects.
   */
  fun resolve(language: GuestLanguage): Set<GuestIntrinsic>
}
