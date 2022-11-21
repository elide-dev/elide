package elide.runtime

/**
 * # Elide: Runtime
 *
 * This object provides hard-coded constants which Elide uses to load packages and perform other key work at runtime. In
 * the [generatedPackage], classes and constants are held which are passed through by the build tools.
 */
public object Runtime {
  /** Package under which build-time values are provided. */
  public const val generatedPackage: String = "elide.runtime.generated"
}
