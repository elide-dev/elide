package elide.runtime

import elide.annotations.Inject
import elide.annotations.Singleton
import io.micronaut.runtime.Micronaut

public interface Engine {
  public fun cylinderCount(): Int
}

@Singleton public class V8 : Engine {
  override fun cylinderCount(): Int = 8
}

public class Injectable {
  @Inject public lateinit var engine: Engine
}

public object Application {
  @JvmStatic public fun main(args: Array<String>) {
    val ctx = Micronaut.build().build()
    val bean = ctx.getBean(Injectable::class.java)
    println("Injectable: $bean")
  }
}
