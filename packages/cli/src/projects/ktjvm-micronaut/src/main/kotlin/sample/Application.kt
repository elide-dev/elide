package sample

import elide.annotations.Inject
import elide.annotations.Singleton
import io.micronaut.core.annotation.Introspected
import io.micronaut.runtime.Micronaut

interface Engine {
  fun cylinderCount(): Int
}

@Singleton @Introspected class V8 : Engine {
  override fun cylinderCount(): Int = 8
}

class Injectable {
  @Inject lateinit var engine: Engine
}

object Application {
  @JvmStatic fun main(args: Array<String>) {
    val ctx = Micronaut.build().build()
    val bean = ctx.getBean(Injectable::class.java)
    println("Injectable: $bean")
  }
}
