package sample

import com.google.common.base.Joiner

fun renderGreeting(): String {
  return Joiner.on(", ").join("Hello", "World")
}

fun main() {
  println(render_greeting())
}
