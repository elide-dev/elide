package elide.runtime.intrinsics.js.stream

/**
 * A strategy used by stream controllers to manage backpressure from compatible sources.
 *
 * This interface is meant to cover both host and guest strategies, providing a clean API for streams to use regardless
 * of the origin of the strategy.
 */
public interface QueueingStrategy {
  /**
   * A high threshold targeted by the controller; once this threshold is reached, no new values will be requested from
   * the source.
   */
  public fun highWaterMark(): Double

  /** Calculate the size of an arbitrary chunk of data. */
  public fun size(chunk: Any?): Double

  /** The default queuing strategy, using a [highWaterMark] of `0.0` and measuring every chunk with size `1.0`. */
  public object Default : QueueingStrategy {
    override fun highWaterMark(): Double = 0.0
    override fun size(chunk: Any?): Double = 1.0
  }
}
