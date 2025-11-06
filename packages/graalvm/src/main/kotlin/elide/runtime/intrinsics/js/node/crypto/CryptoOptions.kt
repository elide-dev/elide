import elide.runtime.intrinsics.js.err.AbstractJsException

/**
 * ## Callback: `crypto.randomInt`
 *
 * Describes the callback function shape which is provided to the `randomInt` operation.
 */
public typealias RandomIntCallback = (err: AbstractJsException?, value: Int?) -> Unit
