package elide.server.runtime.jvm

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.atomic.AtomicBoolean

/** Initializes JVM security providers at server startup. */
@Suppress("UtilityClassWithPublicConstructor")
public object SecurityProviderConfigurator {
  private val ready = AtomicBoolean(false)

  // Register security providers at JVM startup time.
  @JvmStatic @Synchronized private fun registerProviders() {
    var bcposition = 0
    if (Conscrypt.isAvailable()) {
      Security.insertProviderAt(Conscrypt.newProvider(), 0)
      bcposition = 1
    }

    Security.insertProviderAt(
      BouncyCastleProvider(),
      bcposition
    )
  }

  /**
   * Initialize security providers available statically; this method is typically run at server startup.
   */
  @JvmStatic public fun initialize() {
    if (!ready()) {
      ready.compareAndSet(false, true)
      registerProviders()
    }
  }

  /**
   * Indicate whether security providers have initialized.
   */
  @JvmStatic public fun ready(): Boolean {
    return ready.get()
  }
}
