package fullstack.reactssr.ui

import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName

external interface SampleProps: Props {
  var message: String
}

val SampleApp = FC<SampleProps> { props ->
  div {
    className = ClassName("sample-app-container")
    strong {
      +props.message
    }
  }
}
