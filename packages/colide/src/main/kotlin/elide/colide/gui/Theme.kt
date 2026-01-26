/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.colide.gui

/**
 * # Theme System
 *
 * Unified theming for Colide OS GUI components.
 * Supports dark and light themes based on Catppuccin color palette.
 *
 * ## Usage
 * ```kotlin
 * // Use current theme
 * val bgColor = Theme.current.background
 * 
 * // Switch themes
 * Theme.setTheme(Theme.DARK)
 * ```
 */
public object Theme {
    
    /**
     * Theme definition with all colors.
     */
    public data class ThemeColors(
        val name: String,
        val background: Int,
        val surface: Int,
        val surfaceAlt: Int,
        val foreground: Int,
        val foregroundMuted: Int,
        val primary: Int,
        val secondary: Int,
        val accent: Int,
        val success: Int,
        val warning: Int,
        val error: Int,
        val border: Int,
        val selection: Int,
        val cursor: Int,
        // Syntax highlighting
        val syntaxKeyword: Int,
        val syntaxString: Int,
        val syntaxNumber: Int,
        val syntaxComment: Int,
        val syntaxFunction: Int,
        val syntaxType: Int,
        val syntaxOperator: Int
    )
    
    /**
     * Catppuccin Mocha (Dark) theme.
     */
    public val DARK: ThemeColors = ThemeColors(
        name = "Catppuccin Mocha",
        background = 0x001e1e2e,    // Base
        surface = 0x00313244,       // Surface0
        surfaceAlt = 0x0045475a,    // Surface1
        foreground = 0x00cdd6f4,    // Text
        foregroundMuted = 0x00a6adc8, // Subtext0
        primary = 0x0089b4fa,       // Blue
        secondary = 0x00cba6f7,     // Mauve
        accent = 0x0094e2d5,        // Teal
        success = 0x00a6e3a1,       // Green
        warning = 0x00f9e2af,       // Yellow
        error = 0x00f38ba8,         // Red
        border = 0x00585b70,        // Surface2
        selection = 0x40585b70,     // Surface2 with alpha
        cursor = 0x00f5e0dc,        // Rosewater
        syntaxKeyword = 0x00cba6f7, // Mauve
        syntaxString = 0x00a6e3a1,  // Green
        syntaxNumber = 0x00fab387,  // Peach
        syntaxComment = 0x006c7086, // Overlay0
        syntaxFunction = 0x0089b4fa, // Blue
        syntaxType = 0x00f9e2af,    // Yellow
        syntaxOperator = 0x0094e2d5 // Teal
    )
    
    /**
     * Catppuccin Latte (Light) theme.
     */
    public val LIGHT: ThemeColors = ThemeColors(
        name = "Catppuccin Latte",
        background = 0x00eff1f5,    // Base
        surface = 0x00ccd0da,       // Surface0
        surfaceAlt = 0x00bcc0cc,    // Surface1
        foreground = 0x004c4f69,    // Text
        foregroundMuted = 0x006c6f85, // Subtext0
        primary = 0x001e66f5,       // Blue
        secondary = 0x008839ef,     // Mauve
        accent = 0x00179299,        // Teal
        success = 0x0040a02b,       // Green
        warning = 0x00df8e1d,       // Yellow
        error = 0x00d20f39,         // Red
        border = 0x009ca0b0,        // Surface2
        selection = 0x409ca0b0,     // Surface2 with alpha
        cursor = 0x00dc8a78,        // Rosewater
        syntaxKeyword = 0x008839ef, // Mauve
        syntaxString = 0x0040a02b,  // Green
        syntaxNumber = 0x00fe640b,  // Peach
        syntaxComment = 0x008c8fa1, // Overlay0
        syntaxFunction = 0x001e66f5, // Blue
        syntaxType = 0x00df8e1d,    // Yellow
        syntaxOperator = 0x00179299 // Teal
    )
    
    /**
     * High contrast dark theme.
     */
    public val HIGH_CONTRAST: ThemeColors = ThemeColors(
        name = "High Contrast",
        background = 0x00000000,
        surface = 0x00111111,
        surfaceAlt = 0x00222222,
        foreground = 0x00ffffff,
        foregroundMuted = 0x00cccccc,
        primary = 0x0000ffff,
        secondary = 0x00ff00ff,
        accent = 0x0000ff00,
        success = 0x0000ff00,
        warning = 0x00ffff00,
        error = 0x00ff0000,
        border = 0x00ffffff,
        selection = 0x40ffffff,
        cursor = 0x00ffffff,
        syntaxKeyword = 0x00ff00ff,
        syntaxString = 0x0000ff00,
        syntaxNumber = 0x00ffff00,
        syntaxComment = 0x00888888,
        syntaxFunction = 0x0000ffff,
        syntaxType = 0x00ffff00,
        syntaxOperator = 0x00ffffff
    )
    
    private var _current: ThemeColors = DARK
    
    /**
     * Get the current theme.
     */
    public val current: ThemeColors get() = _current
    
    /**
     * Set the current theme.
     */
    public fun setTheme(theme: ThemeColors) {
        _current = theme
    }
    
    /**
     * Toggle between dark and light themes.
     */
    public fun toggle() {
        _current = if (_current == DARK) LIGHT else DARK
    }
    
    /**
     * List available themes.
     */
    public fun availableThemes(): List<ThemeColors> = listOf(DARK, LIGHT, HIGH_CONTRAST)
}

