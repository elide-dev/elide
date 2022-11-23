package elide.tool.ssg

/** Class which holds output fragments and state as they are built within the SSG compiler. */
public class StaticSiteBuffer {
  private val allFragments: ArrayList<StaticFragment> = ArrayList()

  /**
   * Add a compiled [StaticFragment] to the current set of buffered fragments.
   *
   * @param fragment Compiled site fragment to add.
   */
  internal fun add(fragment: StaticFragment) {
    allFragments.add(fragment)
  }
}
