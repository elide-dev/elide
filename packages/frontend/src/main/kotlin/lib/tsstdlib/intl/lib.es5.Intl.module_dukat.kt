@file:JsQualifier("tsstdlib.Intl")
@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
  "DEPRECATION",
)

package lib.tsstdlib.intl

import kotlin.js.*

external interface CollatorOptions {
    var usage: String?
        get() = definedExternally
        set(value) = definedExternally
    var localeMatcher: String?
        get() = definedExternally
        set(value) = definedExternally
    var numeric: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var caseFirst: String?
        get() = definedExternally
        set(value) = definedExternally
    var sensitivity: String?
        get() = definedExternally
        set(value) = definedExternally
    var ignorePunctuation: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

external interface ResolvedCollatorOptions {
    var locale: String
    var usage: String
    var sensitivity: String
    var ignorePunctuation: Boolean
    var collation: String
    var caseFirst: String
    var numeric: Boolean
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
external interface Collator {
    fun compare(x: String, y: String): Number
    fun resolvedOptions(): ResolvedCollatorOptions

    companion object {
        @nativeInvoke
        operator fun invoke(locales: Any /* String | Array<String> */ = definedExternally, options: CollatorOptions = definedExternally): Collator
        fun supportedLocalesOf(locales: Any /* String | Array<String> */, options: CollatorOptions = definedExternally): Array<String>
    }
}

external interface NumberFormatOptions {
    var localeMatcher: String?
        get() = definedExternally
        set(value) = definedExternally
    var style: String?
        get() = definedExternally
        set(value) = definedExternally
    var currency: String?
        get() = definedExternally
        set(value) = definedExternally
    var currencyDisplay: String?
        get() = definedExternally
        set(value) = definedExternally
    var useGrouping: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var minimumIntegerDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var minimumFractionDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var maximumFractionDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var minimumSignificantDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var maximumSignificantDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface ResolvedNumberFormatOptions {
    var locale: String
    var numberingSystem: String
    var style: String
    var currency: String?
        get() = definedExternally
        set(value) = definedExternally
    var currencyDisplay: String?
        get() = definedExternally
        set(value) = definedExternally
    var minimumIntegerDigits: Number
    var minimumFractionDigits: Number
    var maximumFractionDigits: Number
    var minimumSignificantDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var maximumSignificantDigits: Number?
        get() = definedExternally
        set(value) = definedExternally
    var useGrouping: Boolean
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
external interface NumberFormat {
    fun format(value: Number): String
    fun resolvedOptions(): ResolvedNumberFormatOptions

    companion object {
        @nativeInvoke
        operator fun invoke(locales: Any /* String | Array<String> */ = definedExternally, options: NumberFormatOptions = definedExternally): NumberFormat
        fun supportedLocalesOf(locales: Any /* String | Array<String> */, options: NumberFormatOptions = definedExternally): Array<String>
    }
}

external interface DateTimeFormatOptions {
    var localeMatcher: String?
        get() = definedExternally
        set(value) = definedExternally
    var weekday: String?
        get() = definedExternally
        set(value) = definedExternally
    var era: String?
        get() = definedExternally
        set(value) = definedExternally
    var year: String?
        get() = definedExternally
        set(value) = definedExternally
    var month: String?
        get() = definedExternally
        set(value) = definedExternally
    var day: String?
        get() = definedExternally
        set(value) = definedExternally
    var hour: String?
        get() = definedExternally
        set(value) = definedExternally
    var minute: String?
        get() = definedExternally
        set(value) = definedExternally
    var second: String?
        get() = definedExternally
        set(value) = definedExternally
    var timeZoneName: String?
        get() = definedExternally
        set(value) = definedExternally
    var formatMatcher: String?
        get() = definedExternally
        set(value) = definedExternally
    var hour12: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var timeZone: String?
        get() = definedExternally
        set(value) = definedExternally
}

external interface ResolvedDateTimeFormatOptions {
    var locale: String
    var calendar: String
    var numberingSystem: String
    var timeZone: String
    var hour12: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var weekday: String?
        get() = definedExternally
        set(value) = definedExternally
    var era: String?
        get() = definedExternally
        set(value) = definedExternally
    var year: String?
        get() = definedExternally
        set(value) = definedExternally
    var month: String?
        get() = definedExternally
        set(value) = definedExternally
    var day: String?
        get() = definedExternally
        set(value) = definedExternally
    var hour: String?
        get() = definedExternally
        set(value) = definedExternally
    var minute: String?
        get() = definedExternally
        set(value) = definedExternally
    var second: String?
        get() = definedExternally
        set(value) = definedExternally
    var timeZoneName: String?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
external interface DateTimeFormat {
    fun format(date: Date = definedExternally): String
    fun format(): String
    fun format(date: Number = definedExternally): String
    fun resolvedOptions(): ResolvedDateTimeFormatOptions

    companion object {
        @nativeInvoke
        operator fun invoke(locales: Any /* String | Array<String> */ = definedExternally, options: DateTimeFormatOptions = definedExternally): DateTimeFormat
        fun supportedLocalesOf(locales: Any /* String | Array<String> */, options: DateTimeFormatOptions = definedExternally): Array<String>
    }
}
