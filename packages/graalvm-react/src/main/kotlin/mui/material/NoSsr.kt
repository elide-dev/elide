@file:JsModule("@mui/material/NoSsr")

package mui.material

external interface NoSsrProps :
  react.PropsWithChildren {
  override var children: react.ReactNode?
}

/**
 * TBD
 */
@JsName("default")
external val NoSsr: react.FC<NoSsrProps>
