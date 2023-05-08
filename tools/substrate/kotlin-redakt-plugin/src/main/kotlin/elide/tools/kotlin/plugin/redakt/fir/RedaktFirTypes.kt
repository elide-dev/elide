package elide.tools.kotlin.plugin.redakt.fir

import org.jetbrains.kotlin.fir.FirSession

// Provide a predicate matcher.
internal val FirSession.redaktPredicateMatcher: FirRedaktExtensionRegistrar.FirRedaktPredicateMatcher by
  FirSession.sessionComponentAccessor()
