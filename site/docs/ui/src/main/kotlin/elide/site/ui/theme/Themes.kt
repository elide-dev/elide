package elide.site.ui.theme

import csstype.Color
import js.core.jso
import mui.material.PaletteMode
import mui.material.styles.Theme
import mui.material.PaletteMode.dark as darkMode
import mui.material.PaletteMode.light as lightMode
import mui.material.styles.createTheme

/** */
object Themes {
  /** Enumerates theme modes. */
  enum class Mode {
    /** Light theme mode. */
    LIGHT,

    /** Dark theme mode. */
    DARK;

    /** Translate this mode to a [PaletteMode]. */
    val mode: PaletteMode get() = when (this) {
      LIGHT -> lightMode
      DARK -> darkMode
    }

    /** @return Theme corresponding to this mode. */
    val theme: Theme get() = when (this) {
      LIGHT -> Light
      DARK -> Dark
    }

    companion object {
      /** Get the [Mode] for the given [PaletteMode]. */
      fun from(mode: PaletteMode): Mode = when (mode) {
        lightMode -> LIGHT
        darkMode -> DARK
      }
    }
  }

  /** */
  const val colorPrimary = "rgba(90, 0, 255, 1)" // #5a00f

  /** */
  const val colorSecondary = "rgba(155, 9, 171, 1)" // #9b09ab

  /** */
  const val commonWhite = "#fefefe"

  /** */
  const val commonBlack = "#111"

  /** */
  private val systemFonts = listOf(
    "-apple-system",
    "ui-sans-serif",
    "BlinkMacSystemFont",
    "'Helvetica Neue'",
    "'Arial'",
    "sans-serif",
    "'Apple Color Emoji'",
    "'Segoe UI Emoji'",
    "'Segoe UI Symbol'",
  )

  /** */
  val systemFont = systemFonts.joinToString(",")

  /** */
  private val monospaceFonts = listOf(
    "'Jetbrains Mono'",
    "'SF Mono'",
    "ui-monospace",
    "monospace",
  )

  /** */
  val monospaceFont = monospaceFonts.joinToString(",")

  /** @return Color for the current theme. */
  internal fun <R> styled(theme: PaletteMode, light: Theme.() -> R, dark: Theme.() -> R): R {
    return when (theme) {
      PaletteMode.light -> light.invoke(Dark)
      PaletteMode.dark -> dark.invoke(Dark)
    }
  }

  val Light = createTheme(
    jso {
      palette = jso {
        mode = lightMode
        primary = jso {
          main = colorPrimary
          light = colorSecondary
          dark = colorPrimary
          contrastText = commonWhite
        }
        common = jso {
          black = Color(commonBlack)
          white = Color(commonWhite)
        }
      }
    }
  )

  val Dark = createTheme(
    jso {
      palette = jso {
        mode = darkMode
        primary = jso {
          main = colorSecondary
          light = colorSecondary
          dark = colorPrimary
          contrastText = commonWhite
        }
        common = jso {
          black = Color(commonBlack)
          white = Color(commonWhite)
        }
      }
    }
  )
}
