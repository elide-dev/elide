package elide.site

import elide.annotations.data.Sensitive
import elide.ssr.annotations.Props
import elide.vm.annotations.Polyglot
import kotlinx.serialization.Transient

/** Top-level application properties. */
@Props data class AppServerProps(
  /** Name of the page being rendered. */
  @Polyglot val page: String,

  /** CSR nonce to use. */
  @Polyglot @Transient @Sensitive val nonce: String? = null,
)
