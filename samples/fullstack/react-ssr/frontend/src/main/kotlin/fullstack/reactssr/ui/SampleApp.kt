package fullstack.react.ui

import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.strong


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
