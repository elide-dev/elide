
/** @return String-rendered SSR content from Node. */
@OptIn(ExperimentalJsExport::class)
@JsExport fun renderContent(): String {
  return "<strong>Hello, embedded SSR!</strong>"
}
