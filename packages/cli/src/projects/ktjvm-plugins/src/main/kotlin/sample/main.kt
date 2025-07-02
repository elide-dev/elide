package sample

import com.google.common.base.Joiner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Message(val text: String)

fun renderSomeJson(): String {
  val msg = Joiner.on(", ").join("Hello", "World")
  val obj = Message(msg)
  return Json.encodeToString(Message.serializer(), obj)
}

fun main() {
  println(renderSomeJson())
}
