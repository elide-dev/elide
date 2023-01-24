package elide.site

import kotlinx.html.BODY
import kotlinx.html.HEAD
import kotlinx.html.*
import java.net.URI
import java.util.Locale
import java.util.ResourceBundle

/**
 * ## Site Page: Internationalized
 *
 * Specifies an interface which pages can comply with in order to enable internationalization. Strings provided by the
 * methods on this object are typically translated for a given active locale.
 */
interface I18nPage {
  /** Page defaults. */
  object Defaults {
    /** Default locale to use. */
    val locale: Locale = Locale.of("en", "US")
  }

  /** Known localization keys. */
  object KnownKeys {
    /** Page title prefix. */
    const val PAGE_TITLE = "page_title"

    /** Page description. */
    const val PAGE_DESCRIPTION = "page_description"

    /** Main page heading. */
    const val PAGE_HEADING = "page_heading"
  }

  /** Render operator for a [Tag] context. */
  sealed interface TagRenderable<Tag: Any>

  /** Builder for setting up a Linked Data payload. */
  interface LinkedDataBuilder {
    companion object {
      @JvmStatic fun create(): LinkedDataBuilder = LinkedDataBuilderImpl()
    }

    /** @return JSON-serialized linked data. */
    fun serializeJson(): String
  }

  /** Default implementation for a linked data builder context. */
  class LinkedDataBuilderImpl : LinkedDataBuilder {
    override fun serializeJson(): String {
      // @TODO(sgammon): implement
      return "{}"
    }
  }

  /** Renderable which can be reduced to a Linked Data payload. */
  interface LinkedDataRenderable {
    /** @return Callable which renders linked data within the context of a [locale] and [LinkedDataBuilder]. */
    fun linkedData(locale: Locale): (suspend LinkedDataBuilder.() -> Unit)
  }

  /** Render interface which produces document head content. */
  interface HeadRenderable : TagRenderable<HEAD> {
    /** @return Callable which renders within the context of [HEAD], for the provided [Locale]. */
    fun head(locale: Locale): (suspend HEAD.() -> Unit)
  }

  /** Render interface which produces body content. */
  @Suppress("unused") interface BodyRenderable : TagRenderable<BODY> {
    /** @return Callable which renders within the context of [BODY], for the provided [Locale]. */
    fun body(locale: Locale): (suspend BODY.() -> Unit)
  }

  /** OpenGraph info for a given page. */
  data class OpenGraphInfo (
    val title: String,
    val description: String,
    val url: URI,
    val image: URI? = null,
    val type: String? = null,
  ) : HeadRenderable, LinkedDataRenderable {
    /** Render OpenGraph tags in the page HEAD. */
    override fun head(locale: Locale): suspend HEAD.() -> Unit = {
      meta {
        attributes["property"] = "og:url"
        content = this@OpenGraphInfo.url.toString()
      }
      if (!type.isNullOrBlank()) meta {
        attributes["property"] = "og:type"
        content = type
      }
      if (this@OpenGraphInfo.title.isNotBlank()) meta {
        attributes["property"] = "og:title"
        content = this@OpenGraphInfo.title
      }
      if (description.isNotBlank()) meta {
        attributes["property"] = "og:description"
        content = description
      }
      if (image != null) meta {
        attributes["property"] = "og:image"
        content = image.toString()
      }
    }

    /** Render against a linked-data builder. */
    override fun linkedData(locale: Locale): suspend LinkedDataBuilder.() -> Unit = {
      // Nothing at this time.
    }
  }

  /** Twitter info for a given page. */
  data class TwitterInfo (
    val site: String = "@elide_fw",
    val card: String = "summary_large_image",
    val image: URI? = null,
  ) : HeadRenderable, LinkedDataRenderable {
    /** Twitter info, rendered against the provided [locale], for use in the page head. */
    override fun head(locale: Locale): suspend HEAD.() -> Unit = {
      meta {
        content = site
        name = "twitter:site"
      }
      meta {
        content = card
        name = "twitter:card"
      }
      if (image != null) {
        meta {
          content = image.toString()
          name = "twitter:image"
        }
      }
    }

    /** Twitter info, rendered against a linked-data payload. */
    override fun linkedData(locale: Locale): suspend LinkedDataBuilder.() -> Unit = {
      // Nothing at this time.
    }
  }

  // -- Methods: SEO -- //

  /** @return OpenGraph info for this page. */
  fun openGraph(locale: Locale = Defaults.locale): OpenGraphInfo = OpenGraphInfo(
    title = pageTitle(locale),
    url = canonical(locale),
    description = pageHeading(locale),
  )

  /** @return Twitter info for this page. */
  fun twitterInfo(locale: Locale = Defaults.locale): TwitterInfo = TwitterInfo()

  /** @return Canonical URL for this page. */
  fun canonical(locale: Locale = Defaults.locale): URI

  /** @return Keywords for this page. */
  fun keywords(locale: Locale = Defaults.locale): Array<String> = arrayOf(
    "elide",
    "framework",
    "runtime",
    "software",
    "polyglot",
    "kotlin",
    "multiplatform",
    "server",
    "ssr",
    "cli",
    "fullstack",
    "apps",
  )

  /** @return Description for this page. */
  fun description(locale: Locale = Defaults.locale): String = localized(KnownKeys.PAGE_DESCRIPTION) ?: ""

  // -- Methods: Content -- //

  /** @return Title to show for this page. */
  fun pageTitle(locale: Locale = Defaults.locale): String = localized(KnownKeys.PAGE_TITLE, locale) ?: ""

  /** @return Heading to show for this page. */
  fun pageHeading(locale: Locale = Defaults.locale): String = localized(KnownKeys.PAGE_HEADING, locale) ?: ""

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
