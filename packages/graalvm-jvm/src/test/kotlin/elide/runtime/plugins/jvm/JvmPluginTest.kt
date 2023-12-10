package elide.runtime.plugins.jvm

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine

@OptIn(DelicateElideApi::class) @Disabled class JvmPluginTest {
  /** Test-scoped logger. */
  private val logging by lazy { Logging.of(JvmPluginTest::class) }

  /** Temporary directory into which compiled class files will be placed for the guest vm to access. */
  @TempDir private lateinit var tempClasspath: Path

  /** Copy classpath entries used in tests to a temporary directory. */
  @BeforeEach fun unpackSampleClasspath() {
    logging.debug("Unpacking sample classpath")
    classpathResources.forEach { path ->
      logging.debug { "Locating sample classpath resource <$path>" }
      val resource = JvmPluginTest::class.java.getResourceAsStream(path)
      assertNotNull(resource, "should load test resource at <$path>")

      // copy the resource to a new file relative to the temp dir
      val file = tempClasspath.resolve(path.removePrefix("/"))
      logging.debug { "Copying sample classpath resource to temp classpath dir at <$file>" }

      file.createParentDirectories()
      file.writeBytes(resource.readAllBytes())
    }
  }

  /** Acquire a [PolyglotContext] configured with the [Jvm] plugin and custom classpath entries. */
  private fun configureEngine() = PolyglotEngine {
    install(Jvm) {
      classpathEntries.forEach {
        val entry = tempClasspath.resolve(it.removePrefix("/"))

        logging.debug { "Adding guest classpath entry <$entry>" }
        classpath(entry)
      }
    }
  }

  @Test fun testRunEntrypoint() {
    val context = configureEngine().acquire()

    // run the entrypoint from a compiled class file
    assertEquals(
      expected = 0,
      actual = context.runJvm("HelloJava"),
      message = "should receive 0 as exit code from guest when running compiled class file",
    )

    // run the entrypoint from a Jar file
    assertEquals(
      expected = 0,
      actual = context.runJvm("HelloJar"),
      message = "should receive 0 as exit code from guest when using class from Jar",
    )
  }

  private companion object {
    /** Test resources that should be extracted into the temporary guest classpath directory. */
    val classpathResources: Array<String> = arrayOf(
      "/samples/HelloJava.class",
      "/samples/Hello.jar",
    )

    /** Additional guest classpath entries to be added relative to the temporary directory. */
    val classpathEntries: Array<String> = arrayOf(
      "samples",
      "samples/Hello.jar",
    )
  }
}
