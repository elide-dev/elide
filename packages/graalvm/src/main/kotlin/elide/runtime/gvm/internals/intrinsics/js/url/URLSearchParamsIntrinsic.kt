package elide.runtime.gvm.internals.intrinsics.js.url

import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.struct.map.BaseJsMultiMap
import elide.runtime.gvm.internals.intrinsics.js.struct.map.BaseMutableJsMultiMap
import elide.runtime.intrinsics.GuestIntrinsic
import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.js.URLMutableSearchParams as IMutableSearchParams
import elide.runtime.intrinsics.js.URLSearchParams as IURLSearchParams
import org.graalvm.polyglot.Value as GuestValue

/** Implements an intrinsic for the `URLSearchParams` global defined by the WhatWG URL Specification. */
@Suppress("RedundantVisibilityModifier")
@Intrinsic(URLSearchParamsIntrinsic.GLOBAL_URL_SEARCH_PARAMS)
internal class URLSearchParamsIntrinsic : AbstractJsIntrinsic() {
  internal companion object {
    /** Global where the `URL` constructor is mounted. */
    const val GLOBAL_URL_SEARCH_PARAMS = "URLSearchParams"

    // JS symbol for the `URLSearchParams` constructor.
    private val URL_SEARCH_PARAMS_SYMBOL = GLOBAL_URL_SEARCH_PARAMS.asJsSymbol()

    // Parse a JS string as a query-string-formatted set of key-value pairs; the resulting map supports multiple values
    // per key, and is mutable, in addition to being held in a sorted state internally.
    @Suppress("unused", "UNUSED_PARAMETER")
    internal fun parseStringParams(params: String): URLParamsMap {
//      SimpleHttpParameters()
      TODO("not yet implemented")
    }
  }

  /**
   * TBD.
   */
  private interface ExtractableBackingMap {
    /**
     * TBD.
     */
    fun asMap(): URLParamsMap
  }

  /**
   * TBD.
   */
  internal sealed class AbstractURLSearchParams constructor(backingMap: URLParamsMap) :
    BaseJsMultiMap<String, String>(
    backingMap,
    mutable = false,
    sorted = true,
    threadsafe = true,
  ),
    ExtractableBackingMap {
    /** @inheritDoc */
    @Suppress("UNCHECKED_CAST")
    override fun asMap(): URLParamsMap = backingMap as URLParamsMap
  }

  /**
   * TBD.
   */
  internal sealed class AbstractMutableURLSearchParams constructor(
    backingMap: URLParamsMap = URLParamsMap.empty(),
  ) : BaseMutableJsMultiMap<String, String>(
    backingMap,
    threadsafe = true,
    sorted = true,
  ),
  ExtractableBackingMap {
    /** @inheritDoc */
    @Suppress("UNCHECKED_CAST")
    override fun asMap(): URLParamsMap = backingMap as URLParamsMap
  }

  /**
   * TBD.
   */
  public class URLSearchParams private constructor(backingMap: URLParamsMap) :
    AbstractURLSearchParams(backingMap),
    IURLSearchParams {
    /**
     * TBD.
     */
    @Polyglot
    constructor() : this(URLParamsMap.empty())

    /**
     * TBD.
     */
    @Polyglot
    constructor(other: Any?) : this(
        when (other) {
      is GuestValue -> TODO("")
      is String -> TODO("")
      is AbstractURLSearchParams -> other.asMap()
      is AbstractMutableURLSearchParams -> other.asMap()
      else -> URLParamsMap.empty()
    },
      )

    /** @inheritDoc */
    @Polyglot override fun toString(): String = "URLSearchParams(immutable, count=$size)"
  }

  /**
   * TBD.
   */
  public class MutableURLSearchParams private constructor(backingMap: URLParamsMap) :
    AbstractMutableURLSearchParams(backingMap),
    IMutableSearchParams {
    /**
     * TBD.
     */
    @Polyglot
    constructor() : this(URLParamsMap.empty())

    /**
     * TBD.
     */
    @Polyglot
    constructor(other: Any?) : this(
        when (other) {
      is GuestValue -> TODO("")
      is String -> TODO("")
      is AbstractURLSearchParams -> other.asMap()
      is AbstractMutableURLSearchParams -> other.asMap()
      else -> URLParamsMap.empty()
    },
      )

    /**
     * TBD.
     */
    /** @inheritDoc */
    @Polyglot override fun toString(): String = "URLSearchParams(mutable, count=$size)"
  }

  /** @inheritDoc */
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[URL_SEARCH_PARAMS_SYMBOL] = URLSearchParams::class.java
  }
}
