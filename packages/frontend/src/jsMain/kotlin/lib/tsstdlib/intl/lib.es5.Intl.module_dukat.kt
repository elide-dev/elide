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

import kotlin.js.Date

public external interface CollatorOptions {
  public var usage: String?
    get() = definedExternally
    set(value) = definedExternally
  public var localeMatcher: String?
    get() = definedExternally
    set(value) = definedExternally
  public var numeric: Boolean?
    get() = definedExternally
    set(value) = definedExternally
  public var caseFirst: String?
    get() = definedExternally
    set(value) = definedExternally
  public var sensitivity: String?
    get() = definedExternally
    set(value) = definedExternally
  public var ignorePunctuation: Boolean?
    get() = definedExternally
    set(value) = definedExternally
}

public external interface ResolvedCollatorOptions {
  public var locale: String
  public var usage: String
  public var sensitivity: String
  public var ignorePunctuation: Boolean
  public var collation: String
  public var caseFirst: String
  public var numeric: Boolean
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface Collator {
  public fun compare(x: String, y: String): Number
  public fun resolvedOptions(): ResolvedCollatorOptions

  public companion object {
    @nativeInvoke
    public operator fun invoke(locales: Any /* String | Array<String> */ = definedExternally, options: CollatorOptions = definedExternally): Collator
    public fun supportedLocalesOf(locales: Any /* String | Array<String> */, options: CollatorOptions = definedExternally): Array<String>
  }
}

public external interface NumberFormatOptions {
  public var localeMatcher: String?
    get() = definedExternally
    set(value) = definedExternally
  public var style: String?
    get() = definedExternally
    set(value) = definedExternally
  public var currency: String?
    get() = definedExternally
    set(value) = definedExternally
  public var currencyDisplay: String?
    get() = definedExternally
    set(value) = definedExternally
  public var useGrouping: Boolean?
    get() = definedExternally
    set(value) = definedExternally
  public var minimumIntegerDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var minimumFractionDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var maximumFractionDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var minimumSignificantDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var maximumSignificantDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
}

public external interface ResolvedNumberFormatOptions {
  public var locale: String
  public var numberingSystem: String
  public var style: String
  public var currency: String?
    get() = definedExternally
    set(value) = definedExternally
  public var currencyDisplay: String?
    get() = definedExternally
    set(value) = definedExternally
  public var minimumIntegerDigits: Number
  public var minimumFractionDigits: Number
  public var maximumFractionDigits: Number
  public var minimumSignificantDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var maximumSignificantDigits: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var useGrouping: Boolean
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface NumberFormat {
  public fun format(value: Number): String
  public fun resolvedOptions(): ResolvedNumberFormatOptions

  public companion object {
    @nativeInvoke
    public operator fun invoke(locales: Any /* String | Array<String> */ = definedExternally, options: NumberFormatOptions = definedExternally): NumberFormat
    public fun supportedLocalesOf(locales: Any /* String | Array<String> */, options: NumberFormatOptions = definedExternally): Array<String>
  }
}

public external interface DateTimeFormatOptions {
  public var localeMatcher: String?
    get() = definedExternally
    set(value) = definedExternally
  public var weekday: String?
    get() = definedExternally
    set(value) = definedExternally
  public var era: String?
    get() = definedExternally
    set(value) = definedExternally
  public var year: String?
    get() = definedExternally
    set(value) = definedExternally
  public var month: String?
    get() = definedExternally
    set(value) = definedExternally
  public var day: String?
    get() = definedExternally
    set(value) = definedExternally
  public var hour: String?
    get() = definedExternally
    set(value) = definedExternally
  public var minute: String?
    get() = definedExternally
    set(value) = definedExternally
  public var second: String?
    get() = definedExternally
    set(value) = definedExternally
  public var timeZoneName: String?
    get() = definedExternally
    set(value) = definedExternally
  public var formatMatcher: String?
    get() = definedExternally
    set(value) = definedExternally
  public var hour12: Boolean?
    get() = definedExternally
    set(value) = definedExternally
  public var timeZone: String?
    get() = definedExternally
    set(value) = definedExternally
}

public external interface ResolvedDateTimeFormatOptions {
  public var locale: String
  public var calendar: String
  public var numberingSystem: String
  public var timeZone: String
  public var hour12: Boolean?
    get() = definedExternally
    set(value) = definedExternally
  public var weekday: String?
    get() = definedExternally
    set(value) = definedExternally
  public var era: String?
    get() = definedExternally
    set(value) = definedExternally
  public var year: String?
    get() = definedExternally
    set(value) = definedExternally
  public var month: String?
    get() = definedExternally
    set(value) = definedExternally
  public var day: String?
    get() = definedExternally
    set(value) = definedExternally
  public var hour: String?
    get() = definedExternally
    set(value) = definedExternally
  public var minute: String?
    get() = definedExternally
    set(value) = definedExternally
  public var second: String?
    get() = definedExternally
    set(value) = definedExternally
  public var timeZoneName: String?
    get() = definedExternally
    set(value) = definedExternally
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface DateTimeFormat {
  public fun format(date: Date = definedExternally): String
  public fun format(): String
  public fun format(date: Number = definedExternally): String
  public fun resolvedOptions(): ResolvedDateTimeFormatOptions

  public companion object {
    @nativeInvoke
    public operator fun invoke(locales: Any /* String | Array<String> */ = definedExternally, options: DateTimeFormatOptions = definedExternally): DateTimeFormat
    public fun supportedLocalesOf(locales: Any /* String | Array<String> */, options: DateTimeFormatOptions = definedExternally): Array<String>
  }
}
