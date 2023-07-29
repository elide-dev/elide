package elide.site

import elide.annotations.Props
import elide.vm.annotations.Polyglot
import elide.annotations.data.Sensitive
import kotlinx.serialization.Transient

/** Top-level application properties. */
@Props data class AppServerProps(
  /** Name of the page being rendered. */
  @Polyglot val page: String,

  /** CSR nonce to use. */
  @get:Polyglot @Transient @Sensitive val nonce: String? = null,
)
