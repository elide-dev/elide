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

package elide.colide

import elide.colide.shell.ColideShell
import elide.colide.ide.ColideIDE

/**
 * # Colide OS Main Entry Point
 *
 * Entry point for Colide OS running on Elide runtime. This class:
 * - Detects execution mode (bare metal vs hosted)
 * - Initializes native drivers (VESA, keyboard, AI)
 * - Launches appropriate interface (shell or GUI)
 *
 * ## Execution Flow
 * ```
 * Cosmopolitan Boot → Elide Runtime → ColideMain → Shell/GUI
 *                          ↓
 *                    JNI → Rust Drivers → Hardware
 * ```
 *
 * ## Command Line Arguments
 * - `--shell`: Force shell mode (default if no display)
 * - `--gui`: Force GUI mode (requires VESA)
 * - `--ide`: Launch IDE directly
 * - `--help`: Show usage
 */
public object ColideMain {
    
    private var mode: Mode = Mode.AUTO
    
    /**
     * Execution modes.
     */
    public enum class Mode {
        AUTO,   // Detect based on hardware
        SHELL,  // Text-based shell
        GUI,    // Graphical interface
        IDE     // Full IDE
    }
    
    /**
     * Main entry point.
     */
    @JvmStatic
    public fun main(args: Array<String>) {
        parseArgs(args)
        
        printBanner()
        
        if (!initNative()) {
            System.err.println("Warning: Native drivers not available, running in emulation mode")
        }
        
        when (detectMode()) {
            Mode.SHELL -> launchShell()
            Mode.GUI -> launchGui()
            Mode.IDE -> launchIde()
            Mode.AUTO -> launchShell() // Fallback
        }
    }
    
    /**
     * Parse command line arguments.
     */
    private fun parseArgs(args: Array<String>) {
        for (arg in args) {
            when (arg.lowercase()) {
                "--shell", "-s" -> mode = Mode.SHELL
                "--gui", "-g" -> mode = Mode.GUI
                "--ide", "-i" -> mode = Mode.IDE
                "--help", "-h" -> {
                    printUsage()
                    return
                }
            }
        }
    }
    
    /**
     * Print startup banner.
     */
    private fun printBanner() {
        println("""
            |
            |  ╔═══════════════════════════════════════════════════════════╗
            |  ║   ____      _ _     _       ___  ____                     ║
            |  ║  / ___|___ | (_) __| | ___ / _ \/ ___|                    ║
            |  ║ | |   / _ \| | |/ _` |/ _ \ | | \___ \                    ║
            |  ║ | |__| (_) | | | (_| |  __/ |_| |___) |                   ║
            |  ║  \____\___/|_|_|\__,_|\___|\___/|____/                    ║
            |  ║                                                           ║
            |  ║  True Unikernel • Elide Runtime • Bare Metal Ready        ║
            |  ╚═══════════════════════════════════════════════════════════╝
            |
        """.trimMargin())
    }
    
    /**
     * Print usage information.
     */
    private fun printUsage() {
        println("""
            |Colide OS - Elide-Powered Operating System
            |
            |Usage: colide [options]
            |
            |Options:
            |  --shell, -s    Launch in shell mode
            |  --gui, -g      Launch in GUI mode
            |  --ide, -i      Launch IDE directly
            |  --help, -h     Show this help
            |
            |Environment:
            |  COLIDE_MODE    Set default mode (shell, gui, ide)
            |  COLIDE_DEBUG   Enable debug output
            |
            |Architecture:
            |  Cosmo boots it, Elide runs it.
            |  - Boot: Cosmopolitan Libc → UEFI/BIOS
            |  - Runtime: Elide (GraalVM Polyglot)
            |  - Display: VESA Framebuffer
            |  - Input: PS/2 Keyboard + Mouse
            |
        """.trimMargin())
    }
    
    /**
     * Initialize native drivers.
     */
    private fun initNative(): Boolean {
        return try {
            ColideNative.ensureInitialized()
        } catch (e: UnsatisfiedLinkError) {
            println("Native library not found: ${e.message}")
            false
        } catch (e: Exception) {
            println("Native init failed: ${e.message}")
            false
        }
    }
    
    /**
     * Detect best execution mode.
     */
    private fun detectMode(): Mode {
        if (mode != Mode.AUTO) return mode
        
        // Check environment
        val envMode = System.getenv("COLIDE_MODE")?.lowercase()
        when (envMode) {
            "shell" -> return Mode.SHELL
            "gui" -> return Mode.GUI
            "ide" -> return Mode.IDE
        }
        
        // Check if running on bare metal with display
        if (ColideNative.isAvailable) {
            val width = ColideNative.screenWidth()
            val height = ColideNative.screenHeight()
            
            if (width > 0 && height > 0) {
                println("Display detected: ${width}x${height}")
                return Mode.GUI
            }
        }
        
        // Default to shell
        return Mode.SHELL
    }
    
    /**
     * Launch shell interface.
     */
    private fun launchShell() {
        println("Starting Colide Shell...")
        println("Type 'help' for available commands, 'gui' to switch to GUI mode")
        println()
        
        val shell = ColideShell()
        shell.start()
    }
    
    /**
     * Launch GUI interface.
     */
    private fun launchGui() {
        println("Starting Colide GUI...")
        
        if (!ColideNative.isAvailable) {
            println("Error: GUI requires native display drivers")
            println("Falling back to shell mode")
            launchShell()
            return
        }
        
        // For now, launch shell - GUI integration coming in P4
        println("GUI mode - launching shell with graphical output")
        launchShell()
    }
    
    /**
     * Launch IDE interface.
     */
    private fun launchIde() {
        println("Starting Colide IDE...")
        
        if (!ColideNative.isAvailable) {
            println("Error: IDE requires native display drivers")
            println("Falling back to shell mode")
            launchShell()
            return
        }
        
        try {
            ColideIDE.main(emptyArray())
        } catch (e: Exception) {
            println("IDE launch failed: ${e.message}")
            println("Falling back to shell mode")
            launchShell()
        }
    }
    
    /**
     * Get current mode.
     */
    public fun currentMode(): Mode = mode
    
    /**
     * Check if running on bare metal.
     */
    public fun isBareMetal(): Boolean {
        return ColideNative.isAvailable && ColideNative.isMetal()
    }
    
    /**
     * Get system info as map.
     */
    public fun systemInfo(): Map<String, String> = buildMap {
        put("os", "Colide OS")
        put("version", "0.1.0")
        put("runtime", "Elide")
        put("mode", mode.name.lowercase())
        put("bareMetal", isBareMetal().toString())
        
        if (ColideNative.isAvailable) {
            put("screenWidth", ColideNative.screenWidth().toString())
            put("screenHeight", ColideNative.screenHeight().toString())
        }
    }
}
