package elide.site

import java.util.Locale
import java.util.ResourceBundle

/**
 * ## Site Page: Internationalized
 *
 * Specifies an interface which pages can comply with in order to enable internationalization. Strings provided by the
 * methods on this object are typically translated for a given active locale.
 */
internal interface I18nPage {
  /** Page defaults. */
  object Defaults {
    /** Default locale to use. */
    val locale: Locale = Locale.of("en", "US")
  }

  /** Known localization keys. */
  object KnownKeys {
    /** Main page heading. */
    const val PAGE_HEADING = "page_heading"
  }

  // -- Methods: Content -- //

  /** @return Heading to show for this page. */
  fun pageHeading(locale: Locale = Defaults.locale): String? = localized(KnownKeys.PAGE_HEADING, locale)

  // -- Methods: Resources -- //

  /**
   * Retrieve the localized bundle for this page.
   *
   * @param locale Locale to load a value for.
   * @return Resource bundle.
   */
  fun bundle(locale: Locale = Defaults.locale): ResourceBundle

  // -- Methods: Localization -- //

  /**
   * Retrieve a localized value by [key], returning `null` if no value is found.
   *
   * @param key Key for the localized value.
   */
  fun localized(key: String, locale: Locale = Defaults.locale): String? = bundle(locale).getString(key)

  /**
   * Retrieve a localized value by `key`, falling back to a sensible `default`.
   *
   * @param key Key for the localized value.
   * @param default Default value to provide if no value is found.
   * @return Resolved value.
   */
  fun localized(key: String, default: String, locale: Locale = Defaults.locale): String =
    localized(key, locale) ?: default

  /**
   * Retrieve a localized value by [key], falling back to the return value of a [default] lambda.
   *
   * @param key Key for the localized value.
   * @param default Function to run to obtain a default value.
   * @return Resolved value.
   */
  fun localized(key: String, default: () -> String, locale: Locale = Defaults.locale): String =
    localized(key, locale) ?: default.invoke()
}
