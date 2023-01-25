package elide.site.abstract

/** Describes info about a page on the Elide website. */
interface PageInfo {
  /** Short identifying string (for references/keys). */
  val name: String

  /** Label to show in navigation. */
  val label: String

  /** Base path for this page. */
  val path: String

  /** Title that should show for the page. */
  val title: String

  /** Whether to hide the page from nav. */
  val hidden: Boolean

  /** Parent [PageInfo] for this page. */
  val parent: PageInfo?

  /** Child [PageInfo] records under this page. */
  val children: List<PageInfo>

  /** Describes a badge affixed to a page, with a color. */
  val badge: Pair<String, String>?
}
