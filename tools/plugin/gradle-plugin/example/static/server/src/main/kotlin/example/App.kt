package example

import elide.server.annotations.Page
import elide.server.controller.PageController
import io.micronaut.http.annotation.Get
import io.micronaut.runtime.Micronaut

@Suppress("SpreadOperator", "FunctionOnlyReturningConstant")
object App {
    @Page(name = "hello") class HelloPage : PageController() {
        @Get fun hello() = "Hello, world!"
    }

    @JvmStatic fun main(args: Array<String>) {
        Micronaut.run(*args)
    }
}
