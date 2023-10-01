package elide.runtime.core.internals.graalvm

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi

@OptIn(DelicateElideApi::class)
class GraalVMRuntimeTest {
  @Test fun testResolveVersion() {
    val cases = arrayOf(
      "20.0.2+9-jvmci-23.0-b14" to GraalVMRuntime.GVM_23,
      "21+35-jvmci-23.1-b15" to GraalVMRuntime.GVM_23_1,
    )
    
    for ((source, version) in cases) assertEquals(
      expected = version,
      actual = GraalVMRuntime.resolveVersion(source),
      message = "should resolve version $version from '$source'",
    )
  }
}