package elide.embedded

import elide.embedded.http.EmbeddedResponse

/** Alias over an unsafe call type received through the native API. */
public typealias UnsafeCall = Any

/** Alias over an unsafe response type sent through the native API. */
public typealias UnsafeResponse = Any

/** A serial codec used to map calls and responses exchanged through the native API. */
public interface EmbeddedCallCodec {
  /** Decode an [unsafe] call provided by the host application. */
  public fun decode(unsafe: UnsafeCall): EmbeddedCall

  /** Encode a [response] into a form compatible with the host application. */
  public fun encode(response: EmbeddedResponse): UnsafeResponse
}
