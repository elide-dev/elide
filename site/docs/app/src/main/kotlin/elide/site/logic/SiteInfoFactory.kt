package elide.site.logic

import elide.site.ElideSite
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import java.util.Locale
import java.util.function.Supplier

/** Site-level resolver/info factory. */
@Factory class SiteInfoFactory : Supplier<ElideSite.SiteInfo> {
  /** @return Default site info. */
  override fun get(): ElideSite.SiteInfo = ElideSite.defaultInfo

  /** @return Site info resolved for the provided [locale]. */
  @Bean fun resolve(locale: Locale? = null): ElideSite.SiteInfo {
    return if (locale == null) {
      get()
    } else {
      ElideSite.defaultInfo
    }
  }
}
