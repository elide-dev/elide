package elide.runtime.gvm.internals.intrinsics.js.http

import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMutableMultiMap
import elide.runtime.intrinsics.js.MultiMapLike
import elide.runtime.intrinsics.js.http.HttpHeaders
import org.graalvm.polyglot.Value
import io.netty.handler.codec.http.HttpHeaders as NettyHttpHeaders

@JvmInline
internal value class HttpHeadersIntrinsic(
  private val data: MultiMapLike<String, String>
) : HttpHeaders, MultiMapLike<String, String> by data {
  companion object {
    /**
     * Construct a new intrinsic wrapper around a headers map provided by Netty's builtin http decoder.
     */
    fun from(headers: NettyHttpHeaders): HttpHeaders {
      return HttpHeadersIntrinsic(JsMutableMultiMap.fromEntries(headers.entries()))
    }

    /**
     * Construct a new intrinsic wrapper from a map-like value passed from the guest VM.
     */
    fun from(value: Value): HttpHeaders = when {
      // an intrinsic value constructed in the host, it must be an instance of this class
      value.isHostObject -> runCatching { value.`as`(HttpHeadersIntrinsic::class.java) }.getOrElse {
        throw IllegalArgumentException("Unsupported header object type: ${value.metaObject.metaSimpleName}")
      }
      
      // a plain JS object, we can convert it to a map
      value.metaObject.metaQualifiedName == "Object" -> JsMutableMultiMap.empty<String, String>().apply {
        for(member in value.memberKeys) if(member.isNotEmpty()) {
          value.getMember(member)?.takeIf { it.isString }?.let { append(member, it.asString()) }
        }
      }.let(::HttpHeadersIntrinsic)
      
      // a JS map-like object, we can wrap it
      value.hasHashEntries() -> JsMutableMultiMap.empty<String, String>().apply {
        val iterator = value.hashKeysIterator
        while(iterator.hasIteratorNextElement()) {
          val key = iterator.iteratorNextElement.asString()
          val entry = value.getHashValueOrDefault(key, null)?.takeIf { it.isString } ?: continue
          
          append(key, entry.asString())
        }
      }.let(::HttpHeadersIntrinsic)

      else -> throw IllegalArgumentException("Unsupported header object type: ${value.metaObject.metaSimpleName}")
    }
  }
}