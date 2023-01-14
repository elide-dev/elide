package fullstack.react

import browser.document
import react.Fragment
import react.create
import react.dom.client.createRoot
import fullstack.react.ui.SampleApp


fun main() {
  val target = document.getElementById("root")
  val container = if (target == null) {
    val c = document.createElement("div")
    document.body.appendChild(c)
    c
  } else {
    target
  }

  val application = Fragment.create() {
    SampleApp {
      message = "Hello, React!"
    }
  }
  createRoot(container).render(application)
}
