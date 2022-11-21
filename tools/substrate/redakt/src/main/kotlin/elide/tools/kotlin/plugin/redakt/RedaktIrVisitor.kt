package elide.tools.kotlin.plugin.redakt

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName

/** Redakt plugin IR tree visitor; responsible for searching for the subject annotation. */
internal class RedaktIrVisitor(
  private val pluginContext: IrPluginContext,
  private val annotation: FqName,
  private val mask: String,
  private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {
  internal companion object {
    private const val LOG_PREFIX = ":Redakt:"
  }

  // Property which is handled/detected by the plugin.
  private class Property(
    val ir: IrProperty,
    val isRedacted: Boolean,
    val parameter: IrValueParameter,
  )

  /** inheritDoc */
  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    log("Reading <$declaration>")

    val declarationParent = declaration.parent
    if (declarationParent is IrClass /* && declaration.isFakeOverride */ &&
      declaration.isToString()) {
      val primaryConstructor =
        declarationParent.primaryConstructor ?: return super.visitFunctionNew(declaration)
      val constructorParameters =
        primaryConstructor.valueParameters.associateBy { it.name.asString() }

      val properties = mutableListOf<Property>()
      val classIsRedacted = declarationParent.hasAnnotation(annotation)
      var anyRedacted = false
      for (prop in declarationParent.properties) {
        val parameter = constructorParameters[prop.name.asString()] ?: continue
        val isRedacted = prop.isRedacted()
        if (isRedacted) {
          anyRedacted = true
        }
        properties += Property(prop, isRedacted, parameter)
      }
      if (classIsRedacted || anyRedacted) {
        if (!declarationParent.isData) {
          declarationParent.reportError("@Redacted is only supported on data classes!")
          return super.visitFunctionNew(declaration)
        }
        declaration.convertToGeneratedToString(properties, classIsRedacted)
      }
    }
    return super.visitFunctionNew(declaration)
  }

  private fun IrFunction.isToString(): Boolean =
    name.asString() == "toString" &&
      valueParameters.isEmpty() &&
      returnType == pluginContext.irBuiltIns.stringType

  private fun IrFunction.convertToGeneratedToString(
    properties: List<Property>,
    classIsRedacted: Boolean
  ) {
    val parent = parent as IrClass
    origin = RedaktOrigin
    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
      generateToStringMethodBody(
        irClass = parent,
        irFunction = this@convertToGeneratedToString,
        irProperties = properties,
        classIsRedacted = classIsRedacted)
    }
    reflectivelySetFakeOverride(false)
  }

  private fun IrFunction.reflectivelySetFakeOverride(isFakeOverride: Boolean) {
    with(javaClass.getDeclaredField("isFakeOverride")) {
      isAccessible = true
      setBoolean(this@reflectivelySetFakeOverride, isFakeOverride)
    }
  }

  private fun IrProperty.isRedacted(): Boolean {
    return hasAnnotation(annotation)
  }

  /**
   * The actual body of the toString method. Copied from
   * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody]
   * .
   */
  private fun IrBlockBodyBuilder.generateToStringMethodBody(
    irClass: IrClass,
    irFunction: IrFunction,
    irProperties: List<Property>,
    classIsRedacted: Boolean
  ) {
    val irConcat = irConcat()
    irConcat.addArgument(irString(irClass.name.asString() + "("))
    if (classIsRedacted) {
      irConcat.addArgument(irString(mask))
    } else {
      var first = true
      for (property in irProperties) {
        if (!first) irConcat.addArgument(irString(", "))

        irConcat.addArgument(irString(property.ir.name.asString() + "="))

        if (property.isRedacted) {
          irConcat.addArgument(irString(mask))
        } else {
          val irPropertyValue = irGetField(receiver(irFunction), property.ir.backingField!!)

          val param = property.parameter
          val irPropertyStringValue =
            if (param.type.isArray() || param.type.isPrimitiveArray()) {
              irCall(
                context.irBuiltIns.dataClassArrayMemberToStringSymbol,
                context.irBuiltIns.stringType)
                .apply { putValueArgument(0, irPropertyValue) }
            } else {
              irPropertyValue
            }

          irConcat.addArgument(irPropertyStringValue)
        }
        first = false
      }
    }
    irConcat.addArgument(irString(")"))
    +irReturn(irConcat)
  }

  private fun IrBlockBodyBuilder.receiver(irFunction: IrFunction) =
    IrGetValueImpl(irFunction.dispatchReceiverParameter!!)

  private fun IrBlockBodyBuilder.IrGetValueImpl(irParameter: IrValueParameter) = IrGetValueImpl(
    startOffset,
    endOffset,
    irParameter.type,
    irParameter.symbol,
  )

  private fun log(message: String) {
    messageCollector.report(CompilerMessageSeverity.LOGGING, "$LOG_PREFIX $message")
  }

  private fun IrClass.reportError(message: String) {
    val location = file.locationOf(this)
    messageCollector.report(CompilerMessageSeverity.ERROR, "$LOG_PREFIX $message", location)
  }

  /** Finds the line and column of [irElement] within this file. */
  private fun IrFile.locationOf(irElement: IrElement?): CompilerMessageSourceLocation {
    val sourceRangeInfo = fileEntry.getSourceRangeInfo(
      beginOffset = irElement?.startOffset ?: SYNTHETIC_OFFSET,
      endOffset = irElement?.endOffset ?: SYNTHETIC_OFFSET,
    )
    return CompilerMessageLocationWithRange.create(
      path = sourceRangeInfo.filePath,
      lineStart = sourceRangeInfo.startLineNumber + 1,
      columnStart = sourceRangeInfo.startColumnNumber + 1,
      lineEnd = sourceRangeInfo.endLineNumber + 1,
      columnEnd = sourceRangeInfo.endColumnNumber + 1,
      lineContent = null)!!
  }
}
