@file:JsModule("preact-render-to-string")

package preact.ssr

import react.ReactNode

/**
 *
 */
@JsName("default")
public external fun render(
  initialChildren: ReactNode,
): String
