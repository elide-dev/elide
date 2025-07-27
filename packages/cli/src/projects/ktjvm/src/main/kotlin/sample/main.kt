package sample

import com.google.common.base.Joiner
import elide.annotations.Secret
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class MyCoolData(
  @Secret val name: String,
)

fun renderGreeting(): String {
  return Joiner.on(" ").join("Hello", "World")
}

fun renderData(name: String = "World"): MyCoolData {
  return MyCoolData(name = name)
}

fun main() {
  println(Json.encodeToString(renderData()))
  println(renderData().toString())
  println(renderGreeting())
}
