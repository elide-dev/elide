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

package elide.colide.apps

import elide.colide.tui.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * # Browse App
 *
 * Simple text-based web browser for Colide OS.
 * Renders HTML as formatted text in TUI.
 */
public class BrowseApp : TuiApp("Browse") {
    
    private lateinit var urlInput: TuiInput
    private lateinit var contentArea: TuiTextArea
    private lateinit var goButton: TuiButton
    
    private var currentUrl = ""
    private val history = mutableListOf<String>()
    private var historyIndex = -1
    
    override fun onCreate() {
        val urlLabel = TuiLabel(1, 1, "URL:", TuiRenderer.Color.PROMPT)
        addComponent(urlLabel)
        
        urlInput = TuiInput(6, 1, 60, "https://example.com")
        urlInput.focused = true
        addComponent(urlInput)
        focusedIndex = 1
        
        goButton = TuiButton(68, 1, "Go") {
            navigate(urlInput.text)
        }
        addComponent(goButton)
        
        contentArea = TuiTextArea(0, 3, renderer.getWidth(), renderer.getHeight() - 5)
        addComponent(contentArea)
        
        contentArea.appendLine("Welcome to Colide Browser")
        contentArea.appendLine("━".repeat(60))
        contentArea.appendLine("")
        contentArea.appendLine("Enter a URL and press Enter or click Go.")
        contentArea.appendLine("")
        contentArea.appendLine("Features:")
        contentArea.appendLine("  • Basic HTTP/HTTPS requests")
        contentArea.appendLine("  • HTML to text conversion")
        contentArea.appendLine("  • Navigation history (Alt+←/→)")
        contentArea.appendLine("")
        contentArea.appendLine("Note: Networking requires hosted mode or")
        contentArea.appendLine("      working WiFi drivers in bare metal mode.")
        
        setStatus("Colide Browser", "", "Enter=go Tab=nav q=quit")
    }
    
    override fun onKeyPress(key: Int): Boolean {
        when (key) {
            '\n'.code -> {
                if (focusedIndex == 1) {
                    navigate(urlInput.text)
                    return true
                }
            }
            '\t'.code -> {
                when (focusedIndex) {
                    1 -> { urlInput.focused = false; goButton.focused = true; focusedIndex = 2 }
                    2 -> { goButton.focused = false; contentArea.focused = true; focusedIndex = 3 }
                    3 -> { contentArea.focused = false; urlInput.focused = true; focusedIndex = 1 }
                }
                return true
            }
            'b'.code -> {
                goBack()
                return true
            }
            'f'.code -> {
                goForward()
                return true
            }
            'r'.code -> {
                if (currentUrl.isNotEmpty()) {
                    navigate(currentUrl)
                    return true
                }
            }
        }
        return false
    }
    
    private fun navigate(url: String) {
        if (url.isBlank()) return
        
        val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        
        contentArea.clear()
        contentArea.appendLine("Loading: $fullUrl")
        contentArea.appendLine("")
        render()
        
        try {
            val content = fetchUrl(fullUrl)
            val text = htmlToText(content)
            
            currentUrl = fullUrl
            urlInput.text = fullUrl
            
            if (historyIndex < history.size - 1) {
                while (history.size > historyIndex + 1) {
                    history.removeAt(history.size - 1)
                }
            }
            history.add(fullUrl)
            historyIndex = history.size - 1
            
            contentArea.clear()
            for (line in text.lines().take(500)) {
                contentArea.appendLine(line)
            }
            
            setStatus(fullUrl.take(40), "Lines: ${contentArea.lines.size}", "b=back r=reload q=quit")
            
        } catch (e: Exception) {
            contentArea.clear()
            contentArea.appendLine("Error loading page:")
            contentArea.appendLine(e.message ?: "Unknown error")
            contentArea.appendLine("")
            contentArea.appendLine("Make sure you have network connectivity.")
            setStatus("Error", "", "q=quit")
        }
    }
    
    private fun fetchUrl(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Colide/1.0 (Elide Runtime)")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("HTTP $responseCode: ${connection.responseMessage}")
        }
        
        return BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            reader.readText()
        }
    }
    
    private fun htmlToText(html: String): String {
        var text = html
        
        text = text.replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<!--[\\s\\S]*?-->"), "")
        
        text = text.replace(Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE)) { match ->
            "═══ ${match.groupValues[1].trim()} ═══\n\n"
        }
        
        text = text.replace(Regex("<h1[^>]*>([^<]*)</h1>", RegexOption.IGNORE_CASE)) { match ->
            "\n# ${match.groupValues[1].trim()}\n"
        }
        text = text.replace(Regex("<h2[^>]*>([^<]*)</h2>", RegexOption.IGNORE_CASE)) { match ->
            "\n## ${match.groupValues[1].trim()}\n"
        }
        text = text.replace(Regex("<h[3-6][^>]*>([^<]*)</h[3-6]>", RegexOption.IGNORE_CASE)) { match ->
            "\n### ${match.groupValues[1].trim()}\n"
        }
        
        text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        text = text.replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "  • ")
        
        text = text.replace(Regex("<a[^>]*href=\"([^\"]*)\"[^>]*>([^<]*)</a>", RegexOption.IGNORE_CASE)) { match ->
            "${match.groupValues[2].trim()} [${match.groupValues[1]}]"
        }
        
        text = text.replace(Regex("<[^>]+>"), "")
        
        text = text.replace("&nbsp;", " ")
        text = text.replace("&amp;", "&")
        text = text.replace("&lt;", "<")
        text = text.replace("&gt;", ">")
        text = text.replace("&quot;", "\"")
        text = text.replace("&#39;", "'")
        text = text.replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: ""
        }
        
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        text = text.replace(Regex("[ \\t]+"), " ")
        
        return text.trim()
    }
    
    private fun goBack() {
        if (historyIndex > 0) {
            historyIndex--
            val url = history[historyIndex]
            urlInput.text = url
            currentUrl = url
            navigate(url)
        }
    }
    
    private fun goForward() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val url = history[historyIndex]
            urlInput.text = url
            currentUrl = url
            navigate(url)
        }
    }
    
    public companion object {
        @JvmStatic
        public fun main(args: Array<String>) {
            val app = BrowseApp()
            app.run()
        }
    }
}
