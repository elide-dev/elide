package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.intriniscs.server.http.internal.GuestHandler

@DelicateElideApi internal interface HandlerRegistry {
  fun register(key: String, handler: GuestHandler)
  fun resolve(key: String): GuestHandler?
}