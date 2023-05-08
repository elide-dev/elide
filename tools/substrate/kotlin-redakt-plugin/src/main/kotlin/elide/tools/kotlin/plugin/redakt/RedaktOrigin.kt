package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl

/** Origin for Redakt compiler intervention. */
internal object RedaktOrigin : IrDeclarationOriginImpl("GENERATED_REDACTED_CLASS_MEMBER")
