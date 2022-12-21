package elide.server
/** Describes the expected interface for a response rendering object. */
public interface ResponseRenderer<R> {
  /** @return Rendered result. */
  public fun render(): R
}
