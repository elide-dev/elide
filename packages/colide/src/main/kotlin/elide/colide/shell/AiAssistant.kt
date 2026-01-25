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

package elide.colide.shell

import elide.colide.Ai

/**
 * # AI Assistant
 *
 * Elide-integrated AI assistant for Colide OS. Wraps llamafile for local inference
 * with conversation history and system prompts.
 *
 * ## Features
 * - Local inference via llamafile (no network required)
 * - Conversation memory within session
 * - Specialized prompts for shell assistance
 * - Code generation and explanation
 */
public class AiAssistant {
    private val history = mutableListOf<Message>()
    private var systemPrompt = DEFAULT_SYSTEM_PROMPT
    
    /**
     * Message in conversation history.
     */
    public data class Message(
        val role: Role,
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Message roles.
     */
    public enum class Role {
        SYSTEM,
        USER,
        ASSISTANT
    }
    
    /**
     * Ask a question and get a response.
     */
    public fun ask(question: String): String {
        history.add(Message(Role.USER, question))
        
        val prompt = buildPrompt(question)
        val response = Ai.complete(prompt)
        
        if (response.isNotEmpty()) {
            history.add(Message(Role.ASSISTANT, response))
        }
        
        return response.ifEmpty { "I'm sorry, AI is not available right now." }
    }
    
    /**
     * Ask for help with a shell command.
     */
    public fun helpWithCommand(command: String): String {
        val prompt = """
            |The user is working in Colide OS shell and needs help with: $command
            |
            |Provide a brief explanation of what this command does and show example usage.
            |Keep the response concise (under 5 lines).
        """.trimMargin()
        
        return ask(prompt)
    }
    
    /**
     * Explain an error message.
     */
    public fun explainError(error: String): String {
        val prompt = """
            |The user encountered this error in Colide OS: $error
            |
            |Explain what this error means and suggest how to fix it.
            |Keep the response concise (under 5 lines).
        """.trimMargin()
        
        return ask(prompt)
    }
    
    /**
     * Generate code for a task.
     */
    public fun generateCode(task: String, language: String = "kotlin"): String {
        val prompt = """
            |Write $language code for the following task: $task
            |
            |Only output the code, no explanations. Keep it minimal and functional.
        """.trimMargin()
        
        return ask(prompt)
    }
    
    /**
     * Suggest next actions based on context.
     */
    public fun suggestNext(context: String): String {
        val prompt = """
            |Based on the current context: $context
            |
            |Suggest 2-3 things the user might want to do next.
            |Format as a numbered list.
        """.trimMargin()
        
        return ask(prompt)
    }
    
    /**
     * Build full prompt with history context.
     */
    private fun buildPrompt(question: String): String {
        val sb = StringBuilder()
        sb.appendLine(systemPrompt)
        sb.appendLine()
        
        val recentHistory = history.takeLast(MAX_HISTORY * 2)
        for (msg in recentHistory) {
            when (msg.role) {
                Role.USER -> sb.appendLine("User: ${msg.content}")
                Role.ASSISTANT -> sb.appendLine("Assistant: ${msg.content}")
                Role.SYSTEM -> {}
            }
        }
        
        if (question !in history.map { it.content }) {
            sb.appendLine("User: $question")
        }
        sb.appendLine("Assistant:")
        
        return sb.toString()
    }
    
    /**
     * Clear conversation history.
     */
    public fun clearHistory() {
        history.clear()
    }
    
    /**
     * Set a custom system prompt.
     */
    public fun setSystemPrompt(prompt: String) {
        systemPrompt = prompt
    }
    
    /**
     * Get conversation history.
     */
    public fun getHistory(): List<Message> = history.toList()
    
    /**
     * Check if AI is available.
     */
    public fun isAvailable(): Boolean {
        return try {
            Ai.complete("test").isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
    
    public companion object {
        private const val MAX_HISTORY = 5
        
        private val DEFAULT_SYSTEM_PROMPT = """
            |You are an AI assistant integrated into Colide OS, a true unikernel operating system.
            |Colide OS runs directly on hardware using Cosmopolitan Libc, with the Elide runtime
            |providing polyglot execution (Kotlin, JavaScript, Python).
            |
            |Key facts about Colide OS:
            |- Boots via UEFI/BIOS without a traditional OS
            |- Uses VESA framebuffer for graphics (640x480, 32bpp)
            |- PS/2 keyboard and mouse for input
            |- Files stored in /zip/ embedded filesystem
            |- AI runs locally via llamafile (TinyLlama model)
            |
            |Keep responses concise and helpful. Focus on practical assistance.
        """.trimMargin()
        
        private var instance: AiAssistant? = null
        
        /**
         * Get the singleton AI assistant instance.
         */
        @JvmStatic
        public fun getInstance(): AiAssistant {
            if (instance == null) {
                instance = AiAssistant()
            }
            return instance!!
        }
    }
}
