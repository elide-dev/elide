package elide.site.abstract

/** Provides baseline logic and interface compliance for Elide site pages. */
abstract class SitePage protected constructor (val info: PageSpec) : PageInfo by info {
  /** Static specification of page info. */
  data class PageSpec internal constructor (
    override val name: String,
    override val label: String,
    override val path: String,
    override val title: String,
  ) : PageInfo

  /** Sugar constructor for easy extension of [SitePage]. */
  protected constructor (
    name: String,
    label: String,
    path: String,
    title: String,
  ) : this(PageSpec(
    name,
    label,
    path,
    title,
  ))
}
