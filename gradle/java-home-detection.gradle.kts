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

/**
 * Enhanced JAVA_HOME detection for IntelliJ IDEA integration
 * 
 * This script provides robust JAVA_HOME detection across different environments:
 * - Local development (macOS, Linux, Windows)
 * - CI/CD environments
 * - IntelliJ IDEA import scenarios
 * - Docker containers
 * 
 * Addresses GitHub issue: JAVA_HOME not detected automatically in IntelliJ IDEA
 */

/**
 * Detect JAVA_HOME with fallback strategies for better IntelliJ IDEA integration
 */
fun detectJavaHome(): String? {
    // Strategy 1: Environment variables
    val envJavaHome = System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }
    if (envJavaHome != null && File(envJavaHome).exists()) {
        return envJavaHome
    }
    
    val envGraalVmHome = System.getenv("GRAALVM_HOME")?.takeIf { it.isNotBlank() }
    if (envGraalVmHome != null && File(envGraalVmHome).exists()) {
        return envGraalVmHome
    }
    
    // Strategy 2: System properties
    val propJavaHome = System.getProperty("java.home")?.takeIf { it.isNotBlank() }
    if (propJavaHome != null && File(propJavaHome).exists()) {
        return propJavaHome
    }
    
    // Strategy 3: Custom override files (existing Elide pattern)
    val javaHomeFile = File(rootProject.projectDir, ".java-home")
    if (javaHomeFile.exists()) {
        val customJavaHome = javaHomeFile.readText().trim()
        if (customJavaHome.isNotBlank() && File(customJavaHome).exists()) {
            return customJavaHome
        }
    }
    
    val graalvmHomeFile = File(rootProject.projectDir, ".graalvm-home")
    if (graalvmHomeFile.exists()) {
        val customGraalvmHome = graalvmHomeFile.readText().trim()
        if (customGraalvmHome.isNotBlank() && File(customGraalvmHome).exists()) {
            return customGraalvmHome
        }
    }
    
    // Strategy 4: Platform-specific common paths
    val os = System.getProperty("os.name").lowercase()
    val commonPaths = when {
        os.contains("mac") -> listOf(
            "/opt/homebrew/opt/openjdk",
            "/usr/local/opt/openjdk",
            "/Library/Java/JavaVirtualMachines"
        )
        os.contains("linux") -> listOf(
            "/usr/lib/jvm/default-java",
            "/usr/lib/jvm/java-21-openjdk",
            "/usr/lib/jvm/java-21-openjdk-amd64",
            "/usr/lib/jvm/java-21-openjdk-arm64",
            "/usr/lib/jvm/java-22-openjdk",
            "/usr/lib/jvm/java-22-openjdk-amd64",
            "/usr/lib/jvm/java-22-openjdk-arm64",
            "/opt/java/openjdk",
            "/usr/lib/gvm" // Common GraalVM path
        )
        os.contains("windows") -> listOf(
            "C:\\Program Files\\Java",
            "C:\\Program Files\\Eclipse Foundation\\jdk",
            "C:\\Program Files\\GraalVM"
        )
        else -> emptyList()
    }
    
    // Find the best Java version available
    val minJavaVersion = project.findProperty("versions.java.minimum")?.toString()?.toIntOrNull() ?: 21
    
    for (basePath in commonPaths) {
        val baseDir = File(basePath)
        if (baseDir.exists() && baseDir.isDirectory) {
            val javaHomes = baseDir.listFiles()
                ?.filter { it.isDirectory }
                ?.mapNotNull { dir ->
                    val javaExe = File(dir, "bin/java")
                    if (javaExe.exists()) {
                        val version = extractJavaVersion(javaExe)
                        if (version != null && version >= minJavaVersion) {
                            version to dir.absolutePath
                        } else null
                    } else null
                }
                ?.sortedByDescending { it.first } // Prefer higher versions
                
            if (javaHomes?.isNotEmpty() == true) {
                return javaHomes.first().second
            }
        }
    }
    
    // Strategy 5: Use current java.home if it meets version requirements
    propJavaHome?.let { javaHome ->
        val javaExe = File(javaHome, "bin/java")
        if (javaExe.exists()) {
            val version = extractJavaVersion(javaExe)
            if (version != null && version >= minJavaVersion) {
                return javaHome
            }
        }
    }
    
    return null
}

/**
 * Extract Java version from java executable
 */
fun extractJavaVersion(javaExe: File): Int? {
    return try {
        val process = ProcessBuilder(javaExe.absolutePath, "-version")
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        
        // Parse version from output like "openjdk version "17.0.2" or "java version "21.0.1""
        val versionRegex = Regex("""(?:openjdk|java) version "(\d+)""")
        val matchResult = versionRegex.find(output)
        matchResult?.groupValues?.get(1)?.toIntOrNull()
    } catch (e: Exception) {
        null
    }
}

/**
 * Generate appropriate gradle.properties content for detected JAVA_HOME
 */
fun generateGradleProperties(detectedJavaHome: String): String {
    return """
        # Auto-generated JAVA_HOME configuration for IntelliJ IDEA integration
        # Generated on: ${java.time.LocalDateTime.now()}
        
        # Java Home Detection
        org.gradle.java.home=$detectedJavaHome
        
        # Ensure IntelliJ IDEA uses the correct Java version
        systemProp.java.home=$detectedJavaHome
        
        # Configure Gradle daemon
        org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC
        
        # Enable configuration cache for better performance
        org.gradle.configuration-cache=true
    """.trimIndent()
}

// Apply the detection
val detectedJavaHome = detectJavaHome()

if (detectedJavaHome != null) {
    logger.lifecycle("Detected JAVA_HOME: $detectedJavaHome")
    
    // Set system properties for IntelliJ IDEA integration
    System.setProperty("elide.detected.java.home", detectedJavaHome)
    
    // Update gradle.properties in sample projects if they don't exist
    val sampleDirs = listOf("samples/ktjvm", "samples/java", "samples/containers")
    sampleDirs.forEach { samplePath ->
        val sampleDir = File(rootProject.projectDir, samplePath)
        val gradlePropsFile = File(sampleDir, "gradle.properties")
        
        if (sampleDir.exists() && !gradlePropsFile.exists()) {
            gradlePropsFile.writeText(generateGradleProperties(detectedJavaHome))
            logger.lifecycle("Generated gradle.properties for $samplePath")
        }
    }
} else {
    logger.warn("Could not detect suitable JAVA_HOME. IntelliJ IDEA integration may require manual configuration.")
    logger.warn("Please set JAVA_HOME environment variable or create a .java-home file in the project root.")
}