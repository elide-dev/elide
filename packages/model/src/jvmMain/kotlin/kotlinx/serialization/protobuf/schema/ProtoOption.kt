@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

import com.google.protobuf.DescriptorProtos.FileOptions.OptimizeMode
import java.util.EnumSet

/**
 * # Proto Options
 *
 * This interface defines the hook-points for Protocol Buffers option values, both known and custom. Known options are
 * defined within enums and matching type structures, with hook-points provided for custom options which preserve strong
 * typing up to Java & Kotlin.
 *
 * ## Option types
 *
 * Protocol Buffer options may be defined within several contexts, also known as [Target]s in regular Java annotation
 * parlance. These are:
 *
 * - [FileOptions], which are applied at the top-level of a given `.proto` source file.
 * - [MessageOptions], which are applied to a given message type.
 * - [FieldOptions], which are applied to a given field within a message type.
 * - [EnumOptions], which are applied to a given enum type.
 * - [EnumValueOptions], which are applied to a given enum value within an enum type.
 * - [ServiceOptions], which are applied to a given service type.
 * - [MethodOptions], which are applied to a given method within a service type.
 *
 * Each type above is provided with implementation classes for each known option. Each known option associates a generic
 * Java/Kotlin type.
 *
 * ## Known options
 *
 * "Known" options are specified in the Protocol Buffers specification. Syntax versions 2 and 3 are implemented via this
 * schema generator. For all registered options, see the spec section for
 * [Options](https://developers.google.com/protocol-buffers/docs/proto3#options).
 *
 * ## Custom options
 *
 * To generate annotations for a custom proto option, extend the base interface called `Custom` defined for your option
 * target, and then pass an instance of your type to the generator options. For example, to generate a custom option for
 * a message:
 *
 * ```kotlin
 * class MyMessageOption(
 *   override val value: String,
 * ): MessageOptions.Custom<String> {
 *   override val name: String = "my_message_option"
 *   override val imports: Set<String> = setOf(
 *     "some/proto/path/to/import.proto",
 *   )
 * }
 * ```
 *
 * Then, invoke the code generator with your option:
 *
 * ```kotlin
 * ProtoBufSchemaGenerator.generateSchemaText(options = ProtoBufGeneratorOptions.DEFAULTS.copy(
 *   protoOptions = listOf(
 *     MyMessageOption(scope = Scope.Message("some.specific.Message"), "Some option value"),
 *   )
 * ))
 * ```
 *
 * Passed through the code generator, and assuming the specified message `some.specific.Message` exists, this code
 * sample becomes:
 *
 * ```proto
 * syntax = "proto3";
 *
 * package some.specific;
 *
 * import "some/proto/path/to/import.proto";
 *
 * message Message {
 *   option (my_message_option) = "Some option value";
 * }
 * ```
 */
@Suppress("unused") public sealed interface ProtoOption<T: Any> {
  /**
   * ### Option field: `value`.
   *
   * Specifies the typed value to express for this option. This value is converted to a string representation for the
   * purposes of code generation (see [symbolValue]). Typically, developers will want to extend further specified
   * interfaces like [StringOption] or [BooleanOption] which manage this field for them.
   */
  public val value: T?

  /**
   * ### Option field: `symbol`.
   *
   * Defines the option name to express within the generated Protocol Buffer source when this option is applied. `name`
   * is ambiguous because it may be in use for other purposes (for example, for enum-typed options).
   */
  public val symbol: String

  /**
   * ### Option field: `scope`.
   *
   * Specifies the scope at which this option applies. When relevant, matching symbols (checked during code-gen) will
   * apply the option to generated source. In combination with [targets], this dictates when and where options are
   * applied.
   */
  public val scope: Scope

  /**
   * ### Option field: `targets`.
   *
   * Specifies the generated Protocol Buffer types to which this option applies. When relevant, matching symbols
   * (checked during code-gen against this and [scope]) apply the option to generated source. In combination with
   * [scope], this dictates when and where options are applied.
   */
  public val targets: EnumSet<Target>

  /**
   * ### Option field: `imports`.
   *
   * Specifies injected imports which are necessary to sustain use of a given Protocol Buffers option. These imports
   * should be picked up and included by the schema generator in order to properly resolve custom option symbols.
   */
  public val imports: Set<String> get() = emptySet()

  /**
   * ### Option field: `builtin`.
   *
   * Specifies whether this proto option is built-in. Built-in options are resolved from Protobuf sources rather than
   * user code. This option should not be used by external developers.
   */
  public val builtin: Boolean get() = false

  /**
   * ### Option field: `symbolValue`.
   *
   * Defines the option value to express within the generated Protocol Buffer source when this option is applied. This
   * should return a string which can be included directly within the `.proto` file. For example, string values must be
   * double-quoted.
   */
  public val symbolValue: String get() = when (value) {
    null -> ""
    is String -> "\"$value\""
    is Enum<*> -> (value as Enum<*>).name
    else -> value.toString()
  }

  /** @return Whether there is a value present for this option. */
  public val isPresent: Boolean get() = value != null

  /**
   * Render the option specified by this instance, including the option's [symbol] (name) and [symbolValue] (value);
   * symbols without a value should not be rendered.
   *
   * @return String-rendered symbol name and value.
   */
  public fun render(): String = "${symbol}=${symbolValue}"

  // -- Abstract Option Types -- //

  /** Convenience type for a boolean-typed proto option. */
  public interface BooleanOption: ProtoOption<Boolean> {
    override val symbolValue: String get() = (value ?: false).toString()
  }

  /** Convenience type for a string-typed proto option. */
  public interface StringOption: ProtoOption<String> {
    override val symbolValue: String get() = "\"${value ?: ""}\""
  }

  /** Convenience type for an enum-typed proto option. */
  public interface EnumTypeOption<T: Enum<T>>: ProtoOption<T> {
    override val symbolValue: String get() = this.value?.name ?: ""
  }

  /** Defines the expected interface for a "known" (built-in) option. */
  public interface KnownOption<T: Any> : ProtoOption<T> {
    public val option: ProtoOption<T>
    override val builtin: Boolean get() = true
  }

  // -- Auxiliary Option Types -- //

  /** Defines the expected interface for an option symbol (a known option name). */
  public interface SymbolicOption {
    public val symbol: String
  }

  /** Defines the supported "scopes" at which proto-options may be applied. */
  @Suppress("unused", "CanBeParameter", "MemberVisibilityCanBePrivate")
  public sealed class Scope constructor (public val symbol: String) {
    /** Nothing scope: default scope which matches nothing. */
    public class None: Scope("__none__")

    /** Global scope: no applicable scope (only usable with [Target.FILE]). */
    public class Global: Scope("")

    /** "All" scope: Apply this option to all relevant structures (for example, all messages). */
    public class All: Scope("*")

    /** "Package" scope: Apply this option to all relevant structures within a given package. */
    public class Package(public val packageName: String): Scope("package:${packageName}")

    /** "Message" scope: Apply this option to a specific message, or all fields within a message. */
    public class Message(public val messageName: String): Scope("message:${messageName}")

    /** "Enum" scope: Apply this option to a specific enumeration. */
    public class Enum(public val enumName: String): Scope("enum:${enumName}")

    public companion object {
      /** Default scope matching nothing. */
      public val NONE: None = None()

      /** Global scope singleton. */
      public val GLOBAL: Global = Global()

      /** "All" scope singleton. */
      public val ALL: All = All()
    }
  }

  /** Enumerates the proto-type "targets" which can be affixed with options. */
  @Suppress("unused") public enum class Target {
    /** File-targeted options are expressed at the top of a Protocol Buffers file. */
    FILE,

    /** Message-targeted options are expressed at the top-level of a given message structure. */
    MESSAGE,

    /** Field-targeted options are expressed within the context of a single message field. */
    FIELD,

    /** Enum-targeted options are expressed at the top-level of a given enum definition. */
    ENUM,

    /** Enum value-targeted options are expressed within the context of a single enum definition instance. */
    ENUM_VALUE,

    /** Service-targeted options are expressed at the top level of a Protocol Buffers `service` definition. */
    SERVICE,

    /** Method-targeted options are expressed within the context of a single service method. */
    METHOD,
  }

  // -- Known Options: Files -- //

  /** Enumerates all available known [Target.FILE]-level options; see [FileOptions] for easy use. */
  @Suppress("unused") public enum class FileOption constructor (override val symbol: String) : SymbolicOption {
    /** @see [FileOptions.JavaPackage]. */
    JAVA_PACKAGE("java_package"),

    /** @see [FileOptions.JavaOuterClassname]. */
    JAVA_OUTER_CLASSNAME("java_outer_classname"),

    /** @see [FileOptions.JavaMultipleFiles]. */
    JAVA_MULTIPLE_FILES("java_multiple_files"),

    /** @see [FileOptions.JavaGenerateEqualsAndHash]. */
    JAVA_GENERATE_EQUALS_AND_HASH("java_generate_equals_and_hash"),

    /** @see [FileOptions.JavaStringCheckUtf8]. */
    JAVA_STRING_CHECK_UTF8("java_string_check_utf8"),

    /** @see [FileOptions.OptimizeFor]. */
    OPTIMIZE_FOR("optimize_for"),

    /** @see [FileOptions.GoPackage]. */
    GO_PACKAGE("go_package"),

    /** @see [FileOptions.GenericServices]. */
    CC_GENERIC_SERVICES("cc_generic_services"),

    /** @see [FileOptions.GenericServices]. */
    JAVA_GENERIC_SERVICES("java_generic_services"),

    /** @see [FileOptions.GenericServices]. */
    PY_GENERIC_SERVICES("py_generic_services"),

    /** @see [FileOptions.Deprecated]. */
    DEPRECATED("deprecated"),

    /** @see [FileOptions.EnableArenas]. */
    CC_ENABLE_ARENAS("cc_enable_arenas"),

    /** @see [FileOptions.ObjcClassPrefix]. */
    OBJC_CLASS_PREFIX("objc_class_prefix"),

    /** @see [FileOptions.CSharpNamespace]. */
    CSHARP_NAMESPACE("csharp_namespace"),

    /** @see [FileOptions.SwiftPrefix]. */
    SWIFT_PREFIX("swift_prefix"),

    /** @see [FileOptions.PhpClassPrefix]. */
    PHP_CLASS_PREFIX("php_class_prefix"),

    /** @see [FileOptions.PhpNamespace]. */
    PHP_NAMESPACE("php_namespace"),

    /** @see [FileOptions.PhpMetadataNamespace]. */
    PHP_METADATA_NAMESPACE("php_metadata_namespace"),

    /** @see [FileOptions.RubyPackage]. */
    RUBY_PACKAGE("ruby_package"),
  }

  /** Specifies the structure for a "known" (built-in) file option. */
  public sealed class KnownFileOption<T: Any>(
    public val option: FileOption,
    override val value: T,
    override val scope: Scope = Scope.ALL,
    override val symbol: String = option.symbol,
    override val targets: EnumSet<Target> = EnumSet.of(Target.FILE),
    override val builtin: Boolean = true,
  ): ProtoOption<T>

  /**
   * ## File Options
   *
   * This object defines classes and defaults for [Target.FILE]-targeted Protocol Buffer options. File-level options are
   * expressed at the top of a given Protocol Buffers source file.
   */
  public object FileOptions {
    /**
     * ### File option: `java_package`
     *
     * Specify this option to control the Java package for generated Protocol Buffer implementation classes in Java. If
     * unspecified, a sensible default is inferred from the proto package path. From the Protocol Buffers documentation:
     *
     * > In Java and Kotlin, the package is used as the Java package, unless you explicitly provide an option
     * > `java_package` in your .proto file.
     *
     * See [Packages](https://developers.google.com/protocol-buffers/docs/proto3#packages) within the Proto3 docs for
     * more information.
     *
     * @property packageName The Java package name to use for generated Protocol Buffer implementation classes.
     */
    public data class JavaPackage(val packageName: String): KnownFileOption<String>(
      FileOption.JAVA_PACKAGE,
      packageName,
    )

    /**
     * ### File option: `java_outer_classname`
     *
     * Specify this option to control the name of the outermost Java class generated for this file; only relevant if
     * `java_multiple_classes` is `false`, in which case, all classes generated from a given proto source file are
     * expressed as nested classes within the class of this name.
     *
     * If no option is passed, and `java_multiple_classes` is `true`, a sensible default is generated, typically in the
     * form of "<Message>OuterClass". If `java_multiple_classes` is `false`, there is no outermost class, because each
     * expressed structure is generated as a class within the calculated (or provided) `package_path`. From the Protocol
     * Buffers documentation:
     *
     * > The class name (and hence the file name) for the wrapper Java class you want to generate. If no explicit
     * > `java_outer_classname` is specified in the .proto file, the class name will be constructed by converting the
     * > .proto file name to camel-case (so foo_bar.proto becomes FooBar.java). If the `java_multiple_files` option is
     * > disabled, then all other classes/enums/etc. generated for the .proto file will be generated within this outer
     * > wrapper Java class as nested classes/enums/etc. If not generating Java code, this option has no effect.
     *
     * See [Options](https://developers.google.com/protocol-buffers/docs/proto3#packages) within the Proto3 docs for
     * more information.
     *
     * @property className The name of the outermost Java class generated for this file, if applicable.
     */
    public data class JavaOuterClassname(val className: String): KnownFileOption<String>(
      FileOption.JAVA_OUTER_CLASSNAME,
      className,
    )

    /**
     * ### File option: `java_multiple_files`
     *
     * Specify this option to control whether or not multiple Java classes are generated for a given proto source file.
     * If the option is `false`, all classes generated from a given proto source file are expressed as nested classes
     * within a class of the name `java_outer_classname` (or a generated default). If the option is `true`, each Java
     * class generated for a given proto source file is generated as a separate Java class under the `java_package_path`
     * or a generated default. From the Protocol Buffers documentation:
     *
     * > If false, only a single .java file will be generated for this .proto file, and all the Java classes/enums/etc.
     * > generated for the top-level messages, services, and enumerations will be nested inside of an outer class (see
     * > `java_outer_classname`). If true, separate .java files will be generated for each of the Java
     * > classes/enums/etc. generated for the top-level messages, services, and enumerations, and the wrapper Java
     * > lass generated for this .proto file won't contain any nested classes/enums/etc. This is a Boolean option
     * > which defaults to `false`. If not generating Java code, this option has no effect.
     *
     * See [Options](https://developers.google.com/protocol-buffers/docs/proto3#packages) within the Proto3 docs for
     * more information.
     *
     * @property multipleFiles Whether or not multiple Java classes are generated for a given proto source file.
     */
    public data class JavaMultipleFiles(val multipleFiles: Boolean = true): KnownFileOption<Boolean>(
      FileOption.JAVA_MULTIPLE_FILES,
      multipleFiles,
    )

    /**
     * ### File option: `java_generate_equals_and_hash`
     *
     * Specify this option to control whether or not the generated Java classes implement the `equals()` and
     * `hashCode()` methods. After a specific version of the Protobuf SDK this is defaulted to `true`, but to maintain
     * backward compatibility, an option is made available to opt-out; this option is `true` by default and typically
     * does not need to be changed.
     *
     * @property generateEqualsAndHash Whether to generate `equals()` and `hashCode()` methods for generated classes.
     */
    public data class JavaGenerateEqualsAndHash(val generateEqualsAndHash: Boolean = true): KnownFileOption<Boolean>(
      FileOption.JAVA_GENERATE_EQUALS_AND_HASH,
      generateEqualsAndHash,
    )

    /**
     * ### File option: `java_string_check_utf8`
     *
     * Specify this option to control whether or not generated Java code performs extra checks for UTF-8 validity. If
     * string values are found to contain non-UTF-8 byte sequences, they are rejected by the codec. This can be an
     * expensive operation in some circumstances, so it is defaulted to `false`.
     *
     * From the Protocol Buffers documentation:
     *
     * > If set true, then the Java2 code generator will generate code that throws an exception whenever an attempt is
     * > made to assign a non-UTF-8 byte sequence to a string field. Message reflection will do the same. However, an
     * > extension field still accepts non-UTF-8 byte sequences. This option has no effect when used with the lite
     * > runtime.
     *
     * @property checkUtf8 Whether to perform extra UTF-8 checks.
     */
    public data class JavaStringCheckUtf8(val checkUtf8: Boolean = false): KnownFileOption<Boolean>(
      FileOption.JAVA_STRING_CHECK_UTF8,
      checkUtf8,
    )

    /**
     * ### File option: `optimize_for`
     *
     * Specify this option to control the generated code's performance vs. size bias. The default is
     * [OptimizeMode.SPEED] generates code for maximum runtime performance. Alternatively, [OptimizeMode.CODE_SIZE] or
     * [OptimizeMode.LITE_RUNTIME] can be specified for constrained or lite-runtime-only environments.
     *
     * From the Protocol Buffers documentation:
     *
     * > Generated classes can be optimized for speed or code size. `SPEED`: Generate complete code for parsing,
     * > serialization, etc. `CODE_SIZE`: Use ReflectionOps to implement these methods. `LITE_RUNTIME`: Generate code
     * > using MessageList and the lite runtime.
     *
     * @property optimizeFor Optimization setting for the Protobuf code generator.
     */
    public data class OptimizeFor(val optimizeFor: OptimizeMode): KnownFileOption<OptimizeMode>(
      FileOption.OPTIMIZE_FOR,
      optimizeFor,
    )

    /**
     * ### File option: `deprecated`
     *
     * Marks an entire `.proto` file full of structures/enums, etc., as deprecated. In applicable languages and target
     * platforms, this generates corresponding deprecation signals or warnings.
     *
     * From the Protocol Buffers documentation:
     *
     * > Is this file deprecated? Depending on the target platform, this can emit Deprecated annotations for everything
     * > in the file, or it will be completely ignored; in the very least, this is a formalization for deprecating files
     *
     * @property deprecated Whether to consider a given file deprecated; defaults to `true` so that the presence of this
     *   annotation is enough to trigger the option.
     */
    public data class Deprecated(val deprecated: Boolean = true): KnownFileOption<Boolean>(
      FileOption.DEPRECATED,
      deprecated,
    )

    /** Default file-level options to apply. */
    public val DEFAULTS: List<KnownFileOption<*>> = listOf(
      JavaMultipleFiles(true),
      OptimizeFor(OptimizeMode.SPEED),
    )
  }

  // -- Known Options: Messages -- //

  /** Enumerates all available known [Target.MESSAGE]-level options; see [MessageOptions] for easy use. */
  public enum class MessageOption constructor (override val symbol: String) : SymbolicOption {
    /** @see [MessageOptions.MessageSetWireFormat]. */
    MESSAGE_SET_WIRE_FORMAT("message_set_wire_format"),

    /** @see [MessageOptions.NoStandardDescriptorAccessor]. */
    NO_STANDARD_DESCRIPTOR_ACCESSOR("no_standard_descriptor_accessor"),

    /** @see [MessageOptions.Deprecated]. */
    DEPRECATED("deprecated"),

    /** @see [MessageOptions.MapEntry]. */
    MAP_ENTRY("map_entry"),
  }

  /** Specifies the structure for a "known" (built-in) message option. */
  public sealed class KnownMessageOption<T: Any>(
    public val option: MessageOption,
    override val value: T,
    override val symbol: String = option.symbol,
    override val targets: EnumSet<Target> = EnumSet.of(Target.MESSAGE),
    override val builtin: Boolean = true,
  ): ProtoOption<T>

  /**
   * ## Message Options
   *
   * This object defines classes and defaults for [Target.MESSAGE]-targeted Protocol Buffer options. Message-level
   * options are expressed at the top of a given message definition, as a class-level annotation.
   */
  public object MessageOptions {
    /**
     * ### Message option: `message_set_wire_format`
     *
     * From the Protocol Buffers documentation:
     *
     * > Set true to use the old proto1 MessageSet wire format for extensions. This is provided for
     * > backwards-compatibility with the MessageSet wire format. You should not use this for any other reason: It's
     * > less efficient, has fewer features, and is more complicated. Defaults to `false`.
     *
     * @property messageSetWireFormat Whether to use the outdated MessageSet wire format.
     * @property scope Scope at which this message option should be applied.
     */
    public data class MessageSetWireFormat(
      val messageSetWireFormat: Boolean,
      override val scope: Scope,
    ): KnownMessageOption<Boolean>(
      MessageOption.MESSAGE_SET_WIRE_FORMAT,
      messageSetWireFormat,
    )

    /**
     * ### Message option: `no_standard_descriptor_accessor`
     *
     * From the Protocol Buffers documentation:
     *
     * > Disables the generation of the standard "descriptor()" accessor, which can conflict with a field of the same
     * > name. This is meant to make migration from proto1 easier; new code should avoid fields named "descriptor".
     *
     * @property noStandardDescriptorAccessor Whether to suppress generation of the `descriptor()` accessor.
     * @property scope Scope at which this message option should be applied.
     */
    public data class NoStandardDescriptorAccessor(
      val noStandardDescriptorAccessor: Boolean = true,
      override val scope: Scope,
    ): KnownMessageOption<Boolean>(
      MessageOption.NO_STANDARD_DESCRIPTOR_ACCESSOR,
      noStandardDescriptorAccessor,
    )

    /**
     * ### Message option: `deprecated`
     *
     * From the Protocol Buffers documentation:
     *
     * > Is this message deprecated? Depending on the target platform, this can emit Deprecated annotations for the
     * > message, or it will be completely ignored; in the very least, this is a formalization for deprecating messages.
     *
     * @property deprecated Whether to suppress generation of the `descriptor()` accessor.
     * @property scope Scope at which this message option should be applied.
     */
    public data class Deprecated(
      val deprecated: Boolean = true,
      override val scope: Scope,
    ): KnownMessageOption<Boolean>(
      MessageOption.NO_STANDARD_DESCRIPTOR_ACCESSOR,
      deprecated,
    )
  }

  // -- Known Options: Fields -- //

  /** Enumerates all available known [Target.FIELD]-level options; see [FieldOptions] for easy use. */
  public enum class FieldOption constructor (override val symbol: String) : SymbolicOption {
    /** @see [FieldOptions.CType]. */
    CTYPE("ctype"),

    /** @see [FieldOptions.Deprecated]. */
    DEPRECATED("deprecated"),

    /** @see [FieldOptions.JsType]. */
    JSTYPE("jstype"),

    /** @see [FieldOptions.Lazy]. */
    LAZY("lazy"),

    /** @see [FieldOptions.Packed]. */
    PACKED("packed"),

    /** @see [FieldOptions.Weak]. */
    WEAK("weak"),
  }

  /**
   * ## Field Options
   *
   * This object defines classes and defaults for [Target.FIELD]-targeted Protocol Buffer options. Field-level options
   * are expressed inline for a given field definition, within the scope of a given message block.
   */
  public object FieldOptions {
    // @TODO
  }

  // -- Known Options: Enumerations -- //

  /** Enumerates all available known [Target.ENUM]-level options; see [EnumOptions] for easy use. */
  public enum class EnumOption constructor (override val symbol: String) : SymbolicOption {
    /** @see [EnumOptions.Deprecated]. */
    DEPRECATED("deprecated"),

    /** @see [EnumOptions.AllowAlias]. */
    ALLOW_ALIAS("allow_alias"),
  }

  /**
   * ## Enum Options
   *
   * This object defines classes and defaults for [Target.ENUM]-targeted Protocol Buffer options. Enum-level options
   * are expressed at the top of an enumeration block, as a class-level annotation.
   */
  public object EnumOptions {
    // @TODO
  }

  // -- Known Options: Enumeration Values -- //

  /** Enumerates all available known [Target.ENUM_VALUE]-level options; see [EnumValueOptions] for easy use. */
  public enum class EnumValueOption constructor (override val symbol: String) : SymbolicOption {
    DEPRECATED("deprecated"),
  }

  /**
   * ## Enum Value Options
   *
   * This object defines classes and defaults for [Target.ENUM_VALUE]-targeted Protocol Buffer options. Enum value
   * options are expressed within the context of a single enumeration value instance.
   */
  public object EnumValueOptions {}

  // -- Known Options: Services -- //

  /** Enumerates all available known [Target.SERVICE]-level options; see [ServiceOptions] for easy use. */
  public enum class ServiceOption constructor (override val symbol: String) : SymbolicOption {
    DEPRECATED("deprecated"),
  }

  /**
   * ## Service Options
   *
   * This object defines classes and defaults for [Target.SERVICE]-targeted Protocol Buffer options. Service options are
   * expressed at the top-level of generated service and gRPC classes, as a class-level annotation.
   */
  public object ServiceOptions {
    // @TODO
  }

  // -- Known Options: Methods -- //

  /** Enumerates all available known [Target.METHOD]-level options; see [MethodOptions] for easy use. */
  public enum class MethodOption constructor (override val symbol: String) : SymbolicOption {
    DEPRECATED("deprecated"),
    IDEMPOTENCY_LEVEL("idempotency_level"),
  }

  /**
   * ## Method Options
   *
   * This object defines classes and defaults for [Target.METHOD]-targeted Protocol Buffer options. Method options are
   * expressed within the scope of a single service method definition.
   */
  public object MethodOptions {
    // @TODO
  }

  // -- Options: Custom -- //

  // -- Options: All -- //
  public object All {
    /** Default file-level options to apply. */
    public val DEFAULTS: List<ProtoOption<*>> = listOf(
      FileOptions.DEFAULTS,
    ).flatten()
  }
}
