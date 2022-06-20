package fullstack.basic

import kotlinx.browser.document


fun main() {
  val message = "Hello from JavaScript!"
  console.info(message)
  val el = document.createElement("strong")
  el.innerHTML = message
  val br = document.createElement("br")
  document.body?.appendChild(br)
  document.body?.appendChild(el)
}
