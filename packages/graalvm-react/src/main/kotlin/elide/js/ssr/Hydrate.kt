@file:Suppress("unused")

package elide.js.ssr

import react.dom.client.hydrateRoot

/** Alias into React 18+ hydration. */
val hydrate = ::hydrateRoot
