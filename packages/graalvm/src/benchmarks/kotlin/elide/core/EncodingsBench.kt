package elide.core

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit.SECONDS
import elide.util.Base64

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@Timeout(time = 30, timeUnit = SECONDS)
@OutputTimeUnit(NANOSECONDS)
class EncodingsBench {
  companion object {
    private const val SAMPLE_TEXT = "Hello, world!"
    private const val MEDIUM_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
    private const val LONG_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    private val SAMPLE_TEXT_BYTES = SAMPLE_TEXT.toByteArray()
    private val MEDIUM_TEXT_BYTES = MEDIUM_TEXT.toByteArray()
    private val LONG_TEXT_BYTES = LONG_TEXT.toByteArray()
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  private fun base64EncodeBytes(value: ByteArray): ByteArray {
    return Base64.encode(value)
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  private fun base64EncodeString(value: String): String {
    return Base64.encodeToString(value)
  }

  @Benchmark fun base64EncodeBytesShort(): ByteArray {
    return base64EncodeBytes(SAMPLE_TEXT_BYTES)
  }

  @Benchmark fun base64EncodeBytesMedium(): ByteArray {
    return base64EncodeBytes(MEDIUM_TEXT_BYTES)
  }

  @Benchmark fun base64EncodeBytesLong(): ByteArray {
    return base64EncodeBytes(LONG_TEXT_BYTES)
  }

  @Benchmark fun base64EncodeStringShort(): String {
    return base64EncodeString(SAMPLE_TEXT)
  }

  @Benchmark fun base64EncodeStringMedium(): String {
    return base64EncodeString(MEDIUM_TEXT)
  }

  @Benchmark fun base64EncodeStringLong(): String {
    return base64EncodeString(LONG_TEXT)
  }
}
