package sample

import elide.annotations.Inject
import elide.testing.annotations.Test
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlin.test.*

@MicronautTest class ApplicationTest {
  @Inject lateinit var injectable: Injectable

  @Test fun `sample test`() {
    assertNotNull(injectable)
  }
}
