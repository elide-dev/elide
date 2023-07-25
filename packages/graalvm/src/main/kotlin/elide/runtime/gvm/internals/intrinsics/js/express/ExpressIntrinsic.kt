package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.js.express.Express
import elide.runtime.intrinsics.js.express.ExpressApp
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.concurrent.Phaser
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implementation for the [Express] intrinsic, capable of managing the VM context from which the route handlers are
 * passed and guarantee safe multi-threaded execution of said handlers.
 */
@Intrinsic(global = ExpressIntrinsic.GLOBAL_EXPRESS)
internal class ExpressIntrinsic : Express, ExpressContext, AbstractJsIntrinsic() {
  private lateinit var phaser: Phaser
  private lateinit var context: Context
  private val contextLock: Lock = ReentrantLock()

  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    bindings[EXPRESS_SYMBOL] = ProxyExecutable { create() }
  }
  
  override fun initialize(contextHandle: Any, phaserHandle: Any) {
    require(contextHandle is Context) { "contextHandle must be a GraalVM Context instance" }
    require(phaserHandle is Phaser) { "phaserHandle must be a Phaser instance" }
    
    context = contextHandle
    phaser = phaserHandle
  }

  private fun create(): ExpressApp {
    return ExpressAppIntrinsic(this)
  }

  override fun pin() {
    phaser.register()
  }

  override fun unpin() {
    phaser.arriveAndDeregister()
  }

  override fun <T> useGuest(block: Context.() -> T): T = contextLock.withLock {
    context.enter()
    val result = runCatching { context.block() }
    context.leave()
    
    result.getOrThrow()
  }
  
  companion object {
    const val GLOBAL_EXPRESS = "express"
    val EXPRESS_SYMBOL = GLOBAL_EXPRESS.asJsSymbol()
  }
}