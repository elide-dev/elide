package elide.runtime.intrinsics.server.http

import elide.runtime.core.DelicateElideApi

/** The method used by an HTTP request. */
@DelicateElideApi public enum class HttpMethod {
  OPTIONS,
  GET,
  HEAD,
  POST,
  PUT,
  PATCH,
  DELETE,
  TRACE,
  CONNECT,
}