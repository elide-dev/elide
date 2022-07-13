package elide.server.runtime.jvm

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.conscrypt.Conscrypt
import java.security.Security
import java.util.concurrent.atomic.AtomicBoolean

/** Initializes JVM security providers at server startup. */
@Suppress("UtilityClassWithPublicConstructor")
public class SecurityProviderConfigurator {
  public companion object {
    private val ready = AtomicBoolean(false)

    // Register security providers at JVM startup time.
    @JvmStatic @Synchronized public fun registerProviders() {
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

    init {
      if (!ready.get()) {
        ready.compareAndSet(false, true)
        registerProviders()
      }
    }
  }
}
