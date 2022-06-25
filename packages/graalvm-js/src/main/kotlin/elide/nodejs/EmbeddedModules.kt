@file:Suppress("unused")

package elide.nodejs

import NodeJS.Process


@JsModule("process")
@JsNonModule
external val process: Process
