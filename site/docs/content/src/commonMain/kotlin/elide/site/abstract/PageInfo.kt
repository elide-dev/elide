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
}
