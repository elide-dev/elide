package elide.model.util

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.CharMatcher
import com.google.common.collect.ImmutableSortedSet
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.protobuf.DescriptorProtos.FieldOptions
import com.google.protobuf.DescriptorProtos.MessageOptions
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.google.protobuf.Message
import com.google.protobuf.util.FieldMaskUtil
import elide.model.FieldMask
import elide.model.err.InvalidModelType
import elide.model.err.MissingAnnotatedField
import tools.elide.model.CollectionMode
import tools.elide.model.Datamodel
import tools.elide.model.DatapointType
import tools.elide.model.FieldType
import java.io.Serializable
import java.util.EnumSet
import java.util.Objects
import java.util.Optional
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListSet
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream
import javax.annotation.concurrent.ThreadSafe

/**
 * Utility helper class, which is responsible for resolving metadata (based on the core framework annotations) from
 * arbitrary model definitions.
 *
 * Model "metadata," in this case, refers to annotation-based declarations on the protocol buffer schemata themselves.
 * As such, the source for most (if not all) of the data provided by this helper is the [Descriptor] that accompanies a
 * Java-side protobuf model.
 *
 * **Note:** Using this class, or the model layer writ-large, requires the full runtime Protobuf library (the lite
 * runtime for Protobuf in Java does not include descriptors at all, which this class relies on).
 */
@ThreadSafe
@Suppress("WeakerAccess", "unused", "OptionalUsedAsFieldOrParameterType", "MemberVisibilityCanBePrivate")
public object ModelMetadata {
  /** Utility class that points to a specific field, in a specific context. */
  public data class FieldPointer (
    public val depth: Int,
    public val parent: String,
    public val path: String,
    public val base: Descriptor,
    public val field: FieldDescriptor,
  ): Serializable, Comparable<FieldPointer> {
    public companion object {
      /**
       * Wrap the field at the specified name on the provided model.
       *
       * @param model Descriptor for a protocol buffer model.
       * @param name Name of a field to get from the provided buffer model.
       * @return Field pointer wrapping the provided information.
       */
      @JvmStatic public fun fieldAtName(model: Descriptor, name: String): FieldPointer {
        return FieldPointer(
          depth = CharMatcher.`is`('.').countIn(name),
          parent = "",
          path = name,
          base = model,
          field = model.findFieldByName(name),
        )
      }
    }

    internal constructor (
      base: Descriptor,
      parent: String,
      path: String,
      field: FieldDescriptor,
    ): this (
      depth = CharMatcher.`is`('.').countIn(path),
      parent = parent,
      path = path,
      base = base,
      field = field,
    )

    internal constructor (
      base: Descriptor,
      path: String,
      field: FieldDescriptor,
    ): this (
      depth = CharMatcher.`is`('.').countIn(path),
      parent = "",
      path = path,
      base = base,
      field = field,
    )

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || javaClass != other.javaClass) return false
      val (depth1, _, path1, base1) = other as FieldPointer
      return (
        depth == depth1 &&
          com.google.common.base.Objects.equal(path, path1) &&
          com.google.common.base.Objects.equal(base.fullName, base1.fullName)
      )
    }

    override fun hashCode(): Int {
      return com.google.common.base.Objects.hashCode(path, base.fullName)
    }

    override fun compareTo(other: FieldPointer): Int {
      return this.path.compareTo(other.path)
    }
  }

  /** Utility class that holds a [FieldPointer] paired with a corresponding field value. */
  public data class FieldContainer<V> internal constructor (
    public val field: FieldPointer,
    public val value: Optional<V>,
  ): Serializable, Comparable<FieldContainer<V>> {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || javaClass != other.javaClass) return false
      val (field1, value1) = other as FieldContainer<*>
      return field == field1 && value.isPresent == value1.isPresent && value == value1
    }

    override fun hashCode(): Int {
      return com.google.common.base.Objects.hashCode(field, value)
    }

    override fun compareTo(other: FieldContainer<V>): Int {
      return this.field.compareTo(other.field)
    }
  }

  // -- Internals -- //

  /**
   * Match an annotation to a field. If the field is not annotated as such, the method returns `false`.
   *
   * @param field Field to check for the provided annotation.
   * @param annotation Annotation to check for.
   * @return Whether the field is annotated with the provided annotation.
   */
  public fun matchFieldAnnotation(field: FieldDescriptor, annotation: FieldType): Boolean {
    if (field.options.hasExtension(Datamodel.field)) {
      val extension = field.options.getExtension(Datamodel.field)
      return annotation == extension.type
    }
    return false
  }

  /**
   * Match a collection annotation. If the field or model is not annotated as such, the method returns `false`.
   *
   * @param field Field to check for the provided annotation.
   * @param mode Collection mode to check for.
   * @return Whether the field is annotated for the provided collection mode.
   */
  public fun matchCollectionAnnotation(field: FieldDescriptor, mode: CollectionMode): Boolean {
    if (field.options.hasExtension(Datamodel.collection)) {
      val extension = field.options.getExtension(Datamodel.collection)
      return mode == extension.mode
    }
    return false
  }

  /**
   * Resolve a model field within the tree of `descriptor`, where an instance of annotation data of type
   * `ext` is affixed to the field. If the (optional) provided `filter` function agrees, the item is
   * returned to the caller in a [FieldPointer].
   *
   *
   * If the field cannot be found, no exception is raised, and [Optional.empty] is returned. The search may
   * also be conducted in `recursive` mode, which proceeds to examine sub-messages if the requested field cannot
   * be located on the top-level `descriptor`.
   *
   * @param descriptor Descriptor where we should begin our search for the desired property.
   * @param ext Extension the field is annotated with. Only fields annotated with this extension are eligible.
   * @param recursive Whether to search recursively, or just on the top-level instance.
   * @param filter Filter function to dispatch per-found-field. The first one to return `true` wins.
   * @param stack Property stack, filled out as we recursively descend.
   * @param E Generic type of the model extension object.
   * @return Optional, containing either a resolved [FieldPointer], or empty.
   */
  public fun <E> resolveAnnotatedField(
    descriptor: Descriptor,
    ext: GeneratedExtension<FieldOptions, E>,
    recursive: Boolean,
    filter: Optional<Function<E, Boolean>>,
    stack: String
  ): Optional<FieldPointer> {
    Objects.requireNonNull(descriptor, "Cannot resolve field from `null` descriptor.")
    Objects.requireNonNull(ext, "Cannot resolve field from `null` descriptor.")
    Objects.requireNonNull(recursive, "Cannot pass `null` for `recursive` flag.")
    Objects.requireNonNull(filter, "Pass empty optional, not `null`, for field filter parameter.")
    Objects.requireNonNull(stack, "Recursive property stack should not be `null`.")
    for (field in descriptor.fields) {
      if (field.options.hasExtension(ext)) {
        val extension = field.options.getExtension(ext)
        if (filter.isEmpty || filter.get().apply(extension)) return Optional.of(
          FieldPointer(
            descriptor,
            if (stack.isEmpty()) field.name else stack + "." + field.name,
            field
          )
        )
      }

      // should we recurse?
      if (recursive && field.type == FieldDescriptor.Type.MESSAGE) {
        // if so, append the current prop to the stack and give it a shot
        val sub = resolveAnnotatedField(
          field.messageType,
          ext,
          recursive,
          filter,
          if (stack.isEmpty()) field.name else stack + "." + field.name
        )
        if (sub.isPresent) return sub
      }
    }
    return Optional.empty()
  }

  /**
   * Resolve a model field within the tree of `descriptor`, identified by the specified deep `path`. If the
   * (optional) provided `filter` function agrees, the item is returned to the caller in a [FieldPointer].
   *
   *
   * If the field cannot be found, no exception is raised, and [Optional.empty] is returned. The search may
   * also be conducted in `recursive` mode, which proceeds to examine sub-messages if the requested field cannot
   * be located on the top-level `descriptor`.
   *
   * @param original Top-level descriptor where we should begin our search for the desired property.
   * @param descriptor Current-level descriptor we are scanning (for recursion).
   * @param path Deep dotted-path to the field we are being asked to resolve.
   * @param remaining Remaining segments of `path` to follow/compute.
   * @return Optional, containing either a resolved [FieldPointer], or empty.
   * @throws IllegalArgumentException If the provided path is syntactically invalid.
   * @throws IllegalArgumentException If an attempt is made to access a property on a primitive field.
   */
  public fun resolveArbitraryField(
    original: Descriptor,
    descriptor: Descriptor,
    path: String,
    remaining: String
  ): Optional<FieldPointer> {
    Objects.requireNonNull(original, "Cannot resolve field from `null` descriptor.")
    Objects.requireNonNull(descriptor, "Cannot resolve field from `null` descriptor.")
    Objects.requireNonNull(path, "Cannot resolve field from `null` path.")
    Objects.requireNonNull(remaining, "Recursive remaining stack should not be `null`.")
    require(!(remaining.startsWith(".") || remaining.endsWith(".") || remaining.contains(" "))) {
      String.format(
        "Invalid deep-field path '%s'.",
        path
      )
    }
    if (!remaining.contains(".")) {
      // maybe we're lucky and don't need to recurse
      for (field in descriptor.fields) {
        if (remaining == field.name) {
          return Optional.of(
            FieldPointer(
              original,
              path,
              field
            )
          )
        }
      }
    } else {
      // need to recurse
      val segment = remaining.substring(0, remaining.indexOf('.'))
      val messageField = descriptor.findFieldByName(segment)
      if (messageField != null && messageField.type == FieldDescriptor.Type.MESSAGE) {
        // found the next tier
        val subType = messageField.messageType
        val newRemainder = remaining.substring(remaining.indexOf('.') + 1)
        return resolveArbitraryField(
          original,
          subType,
          path,
          newRemainder
        )
      } else require(messageField == null) {
        // it's not a message :(
        String.format(
          "Cannot access sub-field of primitive leaf field, at '%s' on model type '%s'.",
          path,
          original.name
        )
      }
    }
    // property not found
    return Optional.empty()
  }

  /**
   * Splice an arbitrary field `value` at `path` into the provided `builder`. If an empty value
   * ([Optional.empty]) is provided, clear any existing value residing at `path`. In all cases, mutate the
   * existing `builder` rather than returning a copy.
   *
   * @param original Top-level builder, which we hand back at the end.
   * @param builder Builder to splice the value into and return.
   * @param path Path at which the target property resides.
   * @param value Value which we should set the target property to, or clear (if passed [Optional.empty]).
   * @param remaining Remaining properties to recurse down to. Internal use only.
   * @param Builder Builder type which we are operating on for this splice.
   * @param Value Value type which we are operating with for this splice.
   * @return Provided `builder` after being mutated with the specified property value.
   */
  public fun <Builder: Message.Builder, Value> spliceArbitraryField(
    original: Message.Builder,
    builder: Message.Builder,
    path: String,
    value: Optional<Value>,
    remaining: String
  ): Builder {
    Objects.requireNonNull(original, "Cannot splice field into `null` original builder.")
    Objects.requireNonNull(builder, "Cannot splice field into `null` builder.")
    Objects.requireNonNull(path, "Cannot resolve field from `null` path.")
    Objects.requireNonNull(value, "Pass an empty optional, not `null`, for value.")
    Objects.requireNonNull(remaining, "Recursive remaining stack should not be `null`.")
    require(!path.startsWith(".")) {
      String.format(
        "Cannot splice path that starts with `.` (got: '%s').", path
      )
    }
    require(!remaining.startsWith(".")) {
      String.format(
        "Cannot splice path that starts with `.` (got: '%s').", remaining
      )
    }
    val descriptor = builder.descriptorForType
    return if (remaining.isNotEmpty() && !remaining.contains(".")) {
      // thankfully, no need to recurse
      val field = Objects.requireNonNull(
        descriptor.findFieldByName(remaining), String.format("failed to locate field %s", remaining)
      )
      if (value.isPresent) {
        try {
          builder.setField(field, value.get())
        } catch (iae: IllegalArgumentException) {
          throw ClassCastException(
            String.format(
              "Failed to set field '%s': value type mismatch.",
              path
            )
          )
        }
      } else {
        builder.clearField(field)
      }
      @Suppress("UNCHECKED_CAST")
      original as Builder
    } else {
      // we have a sub-message that is initialized, so we need to recurse.
      val segment = remaining.substring(0, remaining.indexOf('.'))
      val newRemainder = remaining.substring(remaining.indexOf('.') + 1)
      spliceArbitraryField(
        original,
        builder.getFieldBuilder(
          Objects.requireNonNull(
            descriptor.findFieldByName(segment), String.format(
              "Failed to locate sub-builder at path '%s' on model '%s'.",
              segment,
              builder.descriptorForType.fullName
            )
          )
        ),
        path,
        value,
        newRemainder
      )
    }
  }

  @VisibleForTesting
  @Suppress("UNCHECKED_CAST")
  internal fun <V: Any> pluckFieldRecursive(
    original: Message,
    instance: Message,
    path: String,
    remaining: String
  ): FieldContainer<V> {
    Objects.requireNonNull(original, "Cannot resolve field from `null` descriptor.")
    Objects.requireNonNull(instance, "Cannot resolve field from `null` instance.")
    Objects.requireNonNull(path, "Cannot resolve field from `null` path.")
    Objects.requireNonNull(remaining, "Recursive remaining stack should not be `null`.")
    val descriptor = instance.descriptorForType
    require(!(remaining.startsWith(".") || remaining.endsWith(".") || remaining.contains(" "))) {
      "Cannot begin or end model property path with `.`"
    }

    if (remaining.isNotEmpty() && !remaining.contains(".")) {
      // we got lucky, no need to recurse
      val field = descriptor.findFieldByName(remaining)
      if (field != null) {
        return if (field.type == FieldDescriptor.Type.MESSAGE) {
          val modelInstance = instance.getField(field) as Message
          FieldContainer(
            FieldPointer(descriptor, path, field),
            if (modelInstance.allFields.isNotEmpty())
              Optional.of(modelInstance as V)
            else Optional.empty()
          )
        } else {
          FieldContainer(
            FieldPointer(descriptor, path, field),
            Optional.of(instance.getField(field) as V)
          )
        }
      }
    } else {
      // find next segment
      val segment = remaining.substring(0, remaining.indexOf('.'))
      val messageField = descriptor.findFieldByName(segment)
      if (messageField != null && messageField.type == FieldDescriptor.Type.MESSAGE) {
        return if (!instance.hasField(messageField)) {
          // there is a sub-message that is not initialized. so the field is technically empty.
          FieldContainer(
            FieldPointer(original.descriptorForType, path, messageField),
            Optional.empty()
          )
        } else {
          // we have a sub-message that is initialized, so we need to recurse.
          val newRemainder = remaining.substring(remaining.indexOf('.') + 1)
          pluckFieldRecursive(
            original,
            instance.getField(messageField) as Message,
            path,
            newRemainder
          )
        }
      } else require(messageField == null) {
        // it's not a message :(
        String.format(
          "Cannot access sub-field of primitive leaf field, at '%s' on model type '%s'.",
          path,
          original.descriptorForType.name
        )
      }
    }
    throw IllegalArgumentException(
      String.format(
        "Failed to locate field '%s' on model type '%s'.",
        path,
        descriptor.name
      )
    )
  }

  // -- Metadata: Qualified Names -- //
  /**
   * Resolve the fully-qualified type path, or name, for the provided datamodel type descriptor. This is essentially
   * syntactic sugar.
   *
   * @param descriptor Model descriptor to resolve a fully-qualified name for.
   * @return Fully-qualified model type name.
   */
  public fun fullyQualifiedName(descriptor: Descriptor): String? {
    return descriptor.fullName
  }

  /**
   * Resolve the fully-qualified type path, or name, for the provided datamodel instance. This method is essentially
   * syntactic sugar for accessing the model instance's descriptor, and then grabbing the fully-qualified name.
   *
   * @param model Model instance to resolve a fully-qualified name for.
   * @return Fully-qualified model type name.
   */
  public fun fullyQualifiedName(model: Message): String? {
    return fullyQualifiedName(model.descriptorForType)
  }

  // -- Metadata: Role Annotations -- //

  /**
   * Resolve the general type for a given datamodel type descriptor. The type is either set by default, or set by an
   * explicit annotation affixed to the protocol buffer definition that backs the model.
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you.
   *
   * @param descriptor Model descriptor to retrieve a type for.
   * @return Type of the provided datamodel.
   */
  public fun role(descriptor: Descriptor): DatapointType {
    return modelAnnotation(descriptor, Datamodel.role, false).orElse(DatapointType.OBJECT)
  }

  /**
   * Resolve the general type for a given datamodel. The type is either set by default, or set by an explicit annotation
   * affixed to the protocol buffer definition that backs the model.
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you.
   *
   * @param model Model to retrieve a type for.
   * @return Type of the provided datamodel.
   */
  public fun role(model: Message): DatapointType {
    Objects.requireNonNull(model, "Cannot resolve type for `null` model.")
    return role(model.descriptorForType)
  }

  /**
   * Enforce that a particular datamodel type matches **any** of the provided [DatapointType] annotations.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param model Model to validate against the provided set of types.
   * @param type Type to enforce for the provided model.
   * @return Whether the provided model is a *member-of* (annotated-by) any of the provided `types`.
   */
  public fun matchRole(model: Message, type: DatapointType): Boolean {
    Objects.requireNonNull<Any>(type, "Cannot match `null` model type.")
    return type == role(model)
  }

  /**
   * Enforce that a particular datamodel schema `descriptor` matches **any** of the provided
   * [DatapointType] annotations.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param descriptor Schema descriptor to validate against the provided set of types.
   * @param type Type to enforce for the provided model.
   * @return Whether the provided model is a *member-of* (annotated-by) any of the provided `types`.
   */
  public fun matchRole(descriptor: Descriptor, type: DatapointType): Boolean {
    Objects.requireNonNull<Any>(type, "Cannot match `null` descriptor type.")
    return type == role(descriptor)
  }

  /**
   * **Check** that a particular datamodel type matches **any** of the provided [DatapointType] annotations.
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param model Model to validate against the provided set of types.
   * @param types Types to validate the model against. If **any** of the provided types match, the check passes.
   * @return Whether the provided model is a *member-of* (annotated-by) any of the provided `types`.
   */
  public fun matchAnyRole(model: Message, vararg types: DatapointType): Boolean {
    Objects.requireNonNull(types, "Cannot match `null` model types.")
    return EnumSet.copyOf(listOf(*types)).contains(role(model))
  }

  /**
   * **Check** that a particular schema `descriptor` matches **any** of the provided [DatapointType]
   * annotations.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param descriptor Schema descriptor to validate against the provided set of types.
   * @param types Types to validate the model against. If **any** of the provided types match, the check passes.
   * @return Whether the provided model is a *member-of* (annotated-by) any of the provided `types`.
   */
  public fun matchAnyRole(descriptor: Descriptor, vararg types: DatapointType): Boolean {
    Objects.requireNonNull(types, "Cannot match `null` model types.")
    return EnumSet.copyOf(listOf(*types)).contains(role(descriptor))
  }

  /**
   * **Enforce** that a particular `model` instance matches the provided [DatapointType] annotation.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param model Model to validate against the provided set of types.
   * @param type Types to validate the model against. If **any** of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in `types`.
   */
  @Throws(InvalidModelType::class)
  public fun enforceRole(model: Message, type: DatapointType) {
    if (!matchRole(model, type))
      throw InvalidModelType.from(model, EnumSet.of(type))
  }

  /**
   * **Enforce** that a particular datamodel schema `descriptor` matches the provided [DatapointType]
   * annotation.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param descriptor Descriptor to validate against the provided set of types.
   * @param type Types to validate the model against. If **any** of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in `types`.
   */
  @Throws(InvalidModelType::class)
  public fun enforceRole(descriptor: Descriptor, type: DatapointType) {
    if (!matchRole(descriptor, type))
      throw InvalidModelType.from(descriptor, EnumSet.of(type))
  }

  /**
   * **Enforce** that a particular datamodel type matches **any** of the provided [DatapointType]
   * annotations.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param model Model to validate against the provided set of types.
   * @param types Types to validate the model against. If **any** of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in `types`.
   */
  @Throws(InvalidModelType::class)
  public fun enforceAnyRole(model: Message, vararg types: DatapointType) {
    if (!matchAnyRole(model, *types))
      throw InvalidModelType.from(model, EnumSet.copyOf(listOf(*types)))
  }

  /**
   * **Enforce** that a particular schema `descriptor` matches **any** of the provided [DatapointType]
   * annotations.
   *
   *
   * [DatapointType] annotations describe the general use case for a given model definition. Is it a database
   * model? A wire model? [DatapointType] will tell you, and this model will sweetly enforce membership amongst
   * a set of types for you.
   *
   * @param descriptor Schema descriptor to validate against the provided set of types.
   * @param types Types to validate the model against. If **any** of the provided types match, the check passes.
   * @throws InvalidModelType If the specified model's type is not included in `types`.
   */
  @Throws(InvalidModelType::class)
  public fun enforceAnyRole(
    descriptor: Descriptor,
    vararg types: DatapointType
  ) {
    if (!matchAnyRole(descriptor, *types)) throw InvalidModelType.from(
      descriptor,
      EnumSet.copyOf(listOf(*types))
    )
  }

  // -- Metadata: Field Resolution -- //
  /**
   * Resolve an arbitrary field pointer from the provided model `instance`, specified by the given `path` to
   * the property. If the property cannot be found, [Optional.empty] is returned.
   *
   *
   * This method is **safe**, in that, unlike other util methods for model metadata, it will not throw if the
   * provided `path` is invalid.
   *
   * @param instance Model instance on which to resolve the specified field.
   * @param path Dotted deep-path to the desired field.
   * @return Resolved field pointer for the requested field, or [Optional.empty].
   */
  public fun resolveField(instance: Message, path: String): Optional<FieldPointer> {
    return resolveField(instance.descriptorForType, path)
  }

  /**
   * Resolve an arbitrary field pointer from the provided model type `escriptor`, specified by the given
   * `path` to the property. If the property cannot be found, [Optional.empty] is returned.
   *
   *
   * This method is **safe**, in that, unlike other util methods for model metadata, it will not throw if the
   * provided `path` is invalid.
   *
   * @param descriptor Model type descriptor on which to resolve the specified field.
   * @param path Dotted deep-path to the desired field.
   * @return Resolved field pointer for the requested field, or [Optional.empty].
   */
  public fun resolveField(descriptor: Descriptor, path: String): Optional<FieldPointer> {
    return resolveArbitraryField(descriptor, descriptor, path, path)
  }

  // -- Metadata: Model Annotations -- //

  /**
   * Retrieve a model-level annotation, from `instance`, structured by `ext`. If no instance of the
   * requested model annotation can be found, [Optional.empty] is returned. Search recursively is supported as
   * well, which descends the search to sub-messages to search for the desired annotation.
   *
   * @param instance Message instance to scan for the specified annotation.
   * @param ext Extension to fetch from the subject model, or any sub-model (if `recursive` is `true`).
   * @param recursive Whether to search recursively for the desired extension.
   * @param E Generic type of extension we are looking for.
   * @return Optional, either [Optional.empty], or wrapping the found extension data instance.
   **/
  public fun <E: Any> modelAnnotation(
    instance: Message,
    ext: GeneratedExtension<MessageOptions, E>,
    recursive: Boolean
  ): Optional<E> {
    return modelAnnotation(instance.descriptorForType, ext, recursive)
  }

  /**
   * Retrieve a model-level annotation, from the provided model schema `descriptor`, structured by `ext`. If
   * no instance of the requested model annotation can be found, [Optional.empty] is returned. Search
   * recursively is supported as well, which descends the search to sub-messages to search for the desired annotation.
   *
   * @param descriptor Schema descriptor for a model type.
   * @param ext Extension to fetch from the subject model, or any sub-model (if `recursive` is `true`).
   * @param recursive Whether to search recursively for the desired extension.
   * @param E Generic type of extension we are looking for.
   * @return Optional, either [Optional.empty], or wrapping the found extension data instance.
   **/
  public fun <E: Any> modelAnnotation(
    descriptor: Descriptor,
    ext: GeneratedExtension<MessageOptions, E>,
    recursive: Boolean
  ): Optional<E> {
    Objects.requireNonNull(descriptor, "Cannot resolve type for `null` descriptor.")
    if (descriptor.options.hasExtension(ext))
      return Optional.of(descriptor.options.getExtension(ext))
    if (recursive) {
      // loop through fields. gather any sub-messages, and check procedurally if any of them match. if we find one that
      // does, we return immediately.
      for (field in descriptor.fields) {
        if (field.type == FieldDescriptor.Type.MESSAGE) {
          val subresult = modelAnnotation(field.messageType, ext, recursive)
          if (subresult.isPresent) return subresult
        }
      }
    }
    return Optional.empty()
  }

  // -- Metadata: Field Annotations -- //

  /**
   * Resolve a [FieldPointer] within the scope of `instance`, that holds values for the specified metadata
   * annotation `ext`. By default, this method searches recursively.
   *
   * @see .annotatedField
   * @see .annotatedField
   * @param instance Model instance to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param E Extension generic type.
   * @return Optional-wrapped field pointer, or [Optional.empty].
   **/
  public fun <E> annotatedField(
    instance: Message,
    ext: GeneratedExtension<FieldOptions, E>
  ): Optional<FieldPointer> {
    return annotatedField(instance, ext, true)
  }

  /**
   * Resolve a [FieldPointer] within the scope of `instance`, that holds values for the specified metadata
   * annotation `ext`.
   *
   *
   * This method variant also allows specifying a **recursive** flag, which, if specified, causes the search to
   * proceed to sub-models (recursively) until a matching field is found. If **recursive** is passed as `false`
   * then the search will only occur at the top-level of `instance`.
   *
   * @see .annotatedField
   * @see .annotatedField
   * @param instance Model instance to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param recursive Whether to conduct this search recursively, or just at the top-level.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or [Optional.empty].
   **/
  public fun <E> annotatedField(
    instance: Message,
    ext: GeneratedExtension<FieldOptions, E>,
    recursive: Boolean
  ): Optional<FieldPointer> {
    return annotatedField(instance, ext, recursive, Optional.empty<Function<E, Boolean>>())
  }

  /**
   * Resolve a [FieldPointer] within the scope of `instance`, that holds values for the specified metadata
   * annotation `ext`.
   *
   *
   * This method variant also allows specifying a **filter**, which will be run for each property encountered with
   * the annotation present. If the filter returns `true`, the field will be selected, otherwise, the search
   * continues until all properties are exhausted (depending on `recursive`).
   *
   * @param instance Model instance to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param recursive Whether to conduct this search recursively, or just at the top-level.
   * @param E Extension generic type.
   * @return Optional-wrapped field pointer, or [Optional.empty].
   **/
  public fun <E> annotatedField(
    instance: Message,
    ext: GeneratedExtension<FieldOptions, E>,
    recursive: Boolean,
    filter: Optional<Function<E, Boolean>>
  ): Optional<FieldPointer> {
    return annotatedField(instance.descriptorForType, ext, recursive, filter)
  }

  /**
   * Resolve a [FieldPointer] within the scope of the provided model `descriptor`, that holds values for the
   * specified metadata annotation `ext`. By default, this search occurs recursively, examining all nested sub-
   * models on the provided instance.
   *
   * @param descriptor Model object descriptor to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or [Optional.empty].
  </E> */
  public fun <E> annotatedField(
    descriptor: Descriptor,
    ext: GeneratedExtension<FieldOptions, E>
  ): Optional<FieldPointer> {
    return annotatedField(descriptor, ext, Optional.empty<Function<E, Boolean>>())
  }

  /**
   * Resolve a [FieldPointer] within the scope of the provided model `descriptor`, that holds values for the
   * specified metadata annotation `ext`. By default, this search occurs recursively, examining all nested sub-
   * models on the provided instance.
   *
   *
   * This method variant also allows specifying a **filter**, which will be run for each property encountered with
   * the annotation present. If the filter returns `true`, the field will be selected, otherwise, the search
   * continues until all properties are exhausted (depending on `recursive`).
   *
   * @param descriptor Model object descriptor to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or [Optional.empty].
  </E> */
  public fun <E> annotatedField(
    descriptor: Descriptor,
    ext: GeneratedExtension<FieldOptions, E>,
    filter: Optional<Function<E, Boolean>>
  ): Optional<FieldPointer> {
    return annotatedField(descriptor, ext, true, filter)
  }

  /**
   * Resolve a [FieldPointer] within the scope of the provided model `descriptor`, that holds values for the
   * specified metadata annotation `ext`. Using the `recursive` parameter, the invoking developer may opt to
   * search for the annotated field recursively.
   *
   *
   * This method variant also allows specifying a **filter**, which will be run for each property encountered with
   * the annotation present. If the filter returns `true`, the field will be selected, otherwise, the search
   * continues until all properties are exhausted (depending on `recursive`).
   *
   * @param descriptor Model object descriptor to search for the specified annotated field on.
   * @param ext Extension (annotation) which should be affixed to the field we are searching for.
   * @param <E> Extension generic type.
   * @return Optional-wrapped field pointer, or [Optional.empty].
  </E> */
  public fun <E> annotatedField(
    descriptor: Descriptor,
    ext: GeneratedExtension<FieldOptions, E>,
    recursive: Boolean,
    filter: Optional<Function<E, Boolean>>
  ): Optional<FieldPointer> {
    return resolveAnnotatedField(descriptor, ext, recursive, filter, "")
  }

  /**
   * Retrieve a field-level annotation, from the provided field schema `descriptor`, structured by `ext`. If
   * no instance of the requested field annotation can be found, [Optional.empty] is returned.
   *
   * @param descriptor Schema descriptor for a field on a model type.
   * @param ext Extension to fetch from the subject field.
   * @param E Generic type of extension we are looking for.
   * @return Optional, either [Optional.empty], or wrapping the found extension data instance.
   **/
  public fun <E: Any> fieldAnnotation(
    descriptor: FieldDescriptor,
    ext: GeneratedExtension<FieldOptions, E>
  ): Optional<E> {
    Objects.requireNonNull(descriptor, "Cannot resolve type for `null` field descriptor.")
    return if (descriptor.options.hasExtension(ext))
      Optional.of(descriptor.options.getExtension(ext))
    else Optional.empty()
  }

  // -- Metadata: ID Fields -- //

  /**
   * Resolve a pointer to the provided model `instance`'s ID field, whether or not it has a value. If there is no
   * ID-annotated field at all, [Optional.empty] is returned. Alternatively, if the model is not compatible with
   * ID fields, an exception is raised (see below).
   *
   * @param instance Model instance for which an ID field is being resolved.
   * @return Optional, either [Optional.empty] or containing a [FieldPointer] to the resolved ID field.
   * @throws InvalidModelType If the specified model does not support IDs. Only objects of type `OBJECT` can be
   * used with this interface.
   */
  @Throws(InvalidModelType::class)
  public fun idField(instance: Message): Optional<FieldPointer> {
    return idField(instance.descriptorForType)
  }

  /**
   * Resolve a pointer to the provided schema type `descriptor`'s ID field, whether or not it has a value. If
   * there is no ID-annotated field at all, [Optional.empty] is returned. Alternatively, if the model is not
   * compatible with ID fields, an exception is raised (see below).
   *
   * @param descriptor Model instance for which an ID field is being resolved.
   * @return Optional, either [Optional.empty] or containing a [FieldPointer] to the resolved ID field.
   * @throws InvalidModelType If the specified model does not support IDs. Only objects of type `OBJECT` can be
   * used with this interface.
   */
  @Throws(InvalidModelType::class)
  public fun idField(descriptor: Descriptor): Optional<FieldPointer> {
    enforceAnyRole(Objects.requireNonNull(descriptor), DatapointType.OBJECT, DatapointType.OBJECT_KEY)
    val topLevelId: Optional<FieldPointer> = Objects.requireNonNull(
      annotatedField(
        descriptor,
        Datamodel.field,
        false,
        Optional.of(Function { field -> field.type === FieldType.ID })
      )
    )
    if (topLevelId.isPresent) {
      return topLevelId
    } else {
      // okay. no top level ID. what about keys, which must be top-level?
      val keyBase = keyField(descriptor)
      if (keyBase.isPresent) {
        // we found a key, so scan the key for an ID, which is required on keys.
        return Objects.requireNonNull(
          resolveAnnotatedField(
            keyBase.get().field.messageType,
            Datamodel.field,
            false,
            Optional.of(Function { field -> field.type === FieldType.ID }),
            keyBase.get().field.name
          )
        )
      }
    }
    // there's no top-level ID, and no top-level key, or the key had no ID. we're done here.
    return Optional.empty()
  }

  // -- Metadata: Key Fields -- //

  /**
   * Resolve a pointer to the provided schema type `descriptor`'s `KEY` field, whether or not it has a value
   * assigned. If there is no key-annotated field at all, [Optional.empty] is returned. Alternatively, if the
   * model is not compatible with key fields, an exception is raised (see below).
   *
   * @param instance Model instance for which a key field is being resolved.
   * @return Optional, either [Optional.empty] or containing a [FieldPointer] to the resolved key field.
   * @throws InvalidModelType If the specified model does not support keys. Only objects of type `OBJECT` can be
   * used with this interface.
   */
  @Throws(InvalidModelType::class)
  public fun keyField(instance: Message): Optional<FieldPointer> {
    return keyField(instance.descriptorForType)
  }

  /**
   * Resolve a pointer to the provided schema type `descriptor`'s `KEY` field, whether or not it has a value
   * assigned. If there is no key-annotated field at all, [Optional.empty] is returned. Alternatively, if the
   * model is not compatible with key fields, an exception is raised (see below).
   *
   * @param descriptor Model type descriptor for which a key field is being resolved.
   * @return Optional, either [Optional.empty] or containing a [FieldPointer] to the resolved key field.
   * @throws InvalidModelType If the specified model does not support keys. Only objects of type `OBJECT` can be
   * used with this interface.
   */
  @Throws(InvalidModelType::class)
  public fun keyField(descriptor: Descriptor): Optional<FieldPointer> {
    enforceAnyRole(Objects.requireNonNull(descriptor), DatapointType.OBJECT)
    return Objects.requireNonNull(
      annotatedField(
        Objects.requireNonNull(descriptor),
        Datamodel.field,
        false,
        Optional.of(Function { field -> field.type === FieldType.KEY })
      )
    )
  }

  // -- Metadata: Value Pluck -- //

  /**
   * Pluck a field value, addressed by a [FieldPointer], from the provided `instance`. If the referenced
   * field is a message, a message instance will be handed back only if there is an initialized value. Leaf fields
   * return their raw value, if set. In all cases, if there is no initialized value, [Optional.empty] is
   * returned.
   *
   * @param instance Model instance from which to pluck the property.
   * @param fieldPointer Pointer to the field we wish to fetch.
   * @param V Generic type of data returned by this operation.
   * @return Optional wrapping the resolved value, or [Optional.empty] if no value could be resolved.
   * @throws IllegalStateException If the referenced property is not found, despite witnessing matching types.
   * @throws IllegalArgumentException If the specified field does not have a matching base type with `instance`.
   **/
  public fun <V: Any> pluck(instance: Message, fieldPointer: FieldPointer): FieldContainer<V> {
    return pluck(instance, fieldPointer.path)
  }

  /**
   * Return a single field value container, plucked from the specified deep `path`, in dot form, using the regular
   * protobuf-definition names for each field. If a referenced field is a message, a message instance will be returned
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, [Optional.empty] is supplied in place.
   *
   * @param instance Model instance to pluck the specified property from.
   * @param path Deep path for the property value we wish to pluck.
   * @param V Expected type for the property. If types do not match, a [ClassCastException] will be raised.
   * @return Field container, either empty, or containing the plucked value.
   * @throws IllegalArgumentException If the provided path is syntactically invalid, or the field does not exist.
   **/
  public fun <V: Any> pluck(instance: Message, path: String): FieldContainer<V> {
    return pluckFieldRecursive(instance, instance, path, path)
  }

  /**
   * Return an iterable containing plucked value containers for each field mentioned in `mask`, that is present on
   * `instance` with an initialized value. If a referenced field is a message, a message instance will be included
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, [Optional.empty] is supplied in place.
   *
   * If a field cannot be found, [Optional.empty] is supplied in its place, so that the output order matches
   * path iteration order on the supplied `mask`. This method is therefore safe with regard to path access.
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @return Stream which emits each field container, with a generic `Object` for each value.
   */
  public fun pluckAll(instance: Message, mask: FieldMask): SortedSet<FieldContainer<Any>> {
    return pluckAll(instance, mask, true)
  }

  /**
   * Return an iterable containing plucked value containers for each field mentioned in `mask`, that is present on
   * `instance` with an initialized value. If a referenced field is a message, a message instance will be included
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, [Optional.empty] is supplied in place.
   *
   * If a field cannot be found, [Optional.empty] is supplied in its place, so that the output order matches
   * path iteration order on the supplied `mask`. This method is therefore safe with regard to path access. If
   * `normalize` is activated (the default for [.pluckAll]), the field mask will be
   * sorted and de-duplicated before processing.
   *
   * Sort order of the return value is based on the full path of properties selected - i.e. field containers are
   * returned in lexicographic sort order matching their underlying property paths.
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @param normalize Whether to normalize the field mask before plucking fields.
   * @return Stream which emits each field container, with a generic `Object` for each value.
   */
  public fun pluckAll(
    instance: Message,
    mask: FieldMask,
    normalize: Boolean
  ): SortedSet<FieldContainer<Any>> {
    return ImmutableSortedSet.copyOfSorted(pluckStream(instance, mask, normalize)
      .collect(Collectors.toCollection { ConcurrentSkipListSet() })
    )
  }

  /**
   * Return a stream which emits plucked value containers for each field mentioned in `mask`, that is present on
   * `instance` with an initialized value. If a referenced field is a message, a message instance will be emitted
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, [Optional.empty] is supplied in place.
   *
   *
   * If a field cannot be found, [Optional.empty] is supplied in its place, so that the output order matches
   * path iteration order on the supplied `mask`. This method is therefore safe with regard to path access.
   *
   *
   * **Performance note:** the [Stream] returned by this method is explicitly parallel-capable, because
   * reading descriptor schema is safely concurrent.
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @return Stream which emits each field container, with a generic `Object` for each value.
   */
  public fun pluckStream(
    instance: Message,
    mask: FieldMask?
  ): Stream<FieldContainer<Any>> {
    return pluckStream(instance, mask, true)
  }

  /**
   * Return a stream which emits plucked value containers for each field mentioned in `mask`, that is present on
   * `instance` with an initialized value. If a referenced field is a message, a message instance will be emitted
   * only if there is an initialized value. Leaf fields return their raw value, if set. In all cases, if there is no
   * initialized value, [Optional.empty] is supplied in place.
   *
   *
   * If a field cannot be found, [Optional.empty] is supplied in its place, so that the output order matches
   * path iteration order on the supplied `mask`. This method is therefore safe with regard to path access. If
   * `normalize` is activated (the default for [.pluckStream]), the field mask will be
   * sorted and de-duplicated before processing.
   *
   *
   * **Performance note:** the [Stream] returned by this method is explicitly parallel-capable, because
   * reading descriptor schema is safely concurrent.
   *
   * @param instance Model instance to pluck the specified properties from.
   * @param mask Mask of properties to pluck from the model instance.
   * @param normalize Whether to normalize the field mask before plucking fields.
   * @return Stream which emits each field container, with a generic `Object` for each value.
   */
  public fun pluckStream(
    instance: Message,
    mask: FieldMask?,
    normalize: Boolean
  ): Stream<FieldContainer<Any>> {
    return TreeSet((if (normalize) FieldMaskUtil.normalize(mask) else mask)!!.pathsList)
      .parallelStream()
      .map { fieldPath: String ->
        pluck(
          instance,
          fieldPath
        )
    }
  }

  // -- Metadata: ID/Key Value Pluck -- //

  /**
   * Resolve the provided model instance's assigned ID, by walking the property structure for the entity, and returning
   * either the first `id`-annotated field's value at the top-level, or the first `id`-annotated field value
   * on the first `key`-annotated message field at the top level of the provided message.
   *
   * If no ID field *value* can be resolved, [Optional.empty] is returned. On the other hand, if the
   * model is not a business object or does not have an ID annotation at all, an exception is raised (see below).
   *
   * @param ID Type for the ID value we are resolving.
   * @param instance Model instance for which an ID value is desired.
   * @return Optional wrapping the value of the model instance's ID, or an empty optional if no value could be resolved.
   * @throws InvalidModelType If the supplied model is not a business object and/or does not have an ID field at all.
   **/
  public fun <ID: Serializable> id(instance: Message): Optional<ID> {
    val descriptor = instance.descriptorForType
    enforceAnyRole(descriptor, DatapointType.OBJECT, DatapointType.OBJECT_KEY)
    val idField = idField(descriptor)
    if (idField.isEmpty) throw MissingAnnotatedField(descriptor, FieldType.ID)
    return pluck<ID>(instance, idField.get()).value
  }

  /**
   * Resolve the provided model instance's assigned `KEY` instance, by walking the property structure for the
   * entity, and returning the first `key`-annotated field's value at the top-level of the provided message.
   *
   * If no key field *value* can be resolved, [Optional.empty] is returned. On the other hand, if the
   * model is not a business object or does not support key annotations at all, an exception is raised (see below).
   *
   * @param Key Type for the key we are resolving.
   * @param instance Model instance for which an key value is desired.
   * @return Optional wrapping the value of the model instance's key, or an empty optional if none could be resolved.
   * @throws InvalidModelType If the supplied model is not a business object and/or does not have an key field at all.
   **/
  public fun <Key: Message> key(instance: Message): Optional<Key> {
    val descriptor = instance.descriptorForType
    enforceRole(descriptor, DatapointType.OBJECT)
    val keyField: Optional<FieldPointer> = annotatedField(
      descriptor,
      Datamodel.field,
      false,
      Optional.of(Function { field -> field.type === FieldType.KEY })
    )
    if (keyField.isEmpty) throw MissingAnnotatedField(descriptor, FieldType.KEY)
    return pluck<Key>(instance, keyField.get()).value
  }

  // -- Metadata: Value Splice -- //

  /**
   * Splice the provided optional value (or clear any existing value) at the field `path` in the provided model
   * `instance`. Return a re-built message after the splice.
   *
   * If [Optional.empty] is passed for the `value` to set, any existing value placed in that field
   * will be cleared. This method works identically for primitive leaf fields and message fields.
   *
   * @param instance Model instance to splice the value into.
   * @param path Deep path at which to splice the value.
   * @param value Value to splice into the model, or [Optional.empty] to clear any existing value.
   * @param Model Model type which we are working with for this splice operation.
   * @param Value Value type which we are splicing in, if applicable.
   * @return Re-built model, after the splice operation.
   **/
  public fun <Model: Message, Value> splice(
    instance: Message,
    path: String,
    value: Optional<Value>
  ): Model {
    return splice(
      instance,
      resolveField(instance, path)
        .orElseThrow {
          IllegalArgumentException(
            String.format(
              "Failed to resolve path '%s' on model instance of type '%s' for value splice.",
              path,
              instance.descriptorForType.name
            )
          )
        },
      value
    )
  }

  /**
   * Splice the provided optional value (or clear any existing value) at the specified `field` pointer, in the
   * provided model `instance`. Return a re-built message after the splice.
   *
   * If [Optional.empty] is passed for the `value` to set, any existing value placed in that field
   * will be cleared. This method works identically for primitive leaf fields and message fields.
   *
   * @param instance Model instance to splice the value into.
   * @param field Resolved and validated field pointer for the field to splice.
   * @param value Value to splice into the model, or [Optional.empty] to clear any existing value.
   * @param Model Model type which we are working with for this splice operation.
   * @param Value Value type which we are splicing in, if applicable.
   * @return Re-built model, after the splice operation.
   **/
  public fun <Model : Message, Value> splice(
    instance: Message,
    field: FieldPointer,
    value: Optional<Value>
  ): Model {
    @Suppress("UNCHECKED_CAST")
    return spliceBuilder<Message.Builder, Value>(instance.toBuilder(), field, value).build() as Model
  }

  /**
   * Splice the provided optional value (or clear any existing value) at the specified `field` pointer, in the
   * provided model `instance`. Return the provided builder after the splice operation. The return value may be
   * ignored if the developer so wishes (the provided `builder` is mutated in place).
   *
   * If [Optional.empty] is passed for the `value` to set, any existing value placed in that field
   * will be cleared. This method works identically for primitive leaf fields and message fields.
   *
   * @param builder Model builder to splice the value into.
   * @param field Resolved and validated field pointer for the field to splice.
   * @param value Value to splice into the model, or [Optional.empty] to clear any existing value.
   * @param Builder Model builder type which we are working with for this splice operation.
   * @param Value Value type which we are splicing in, if applicable.
   * @return Model `builder`, after the splice operation.
   **/
  @CanIgnoreReturnValue
  public fun <Builder: Message.Builder, Value> spliceBuilder(
    builder: Message.Builder,
    field: FieldPointer,
    value: Optional<Value>
  ): Builder {
    val noPrefixPath = if (field.path.startsWith("."))
      field.path.substring(1)
    else field.path
    return spliceArbitraryField(
      builder,
      builder,
      noPrefixPath,
      value,
      noPrefixPath
    )
  }

  // -- Metadata: ID/Key Splice -- //

  /**
   * Splice the provided value at `val`, into the ID field value for `instance`. If an ID-annotated property
   * cannot be located, or the model is not of a suitable/type role for use with IDs, an exception is raised (see below
   * for more info).
   *
   * If an existing value exists for the model's ID, **it will be replaced**. In most object-based storage engines
   * this will end up copying the object, rather than mutating an ID. Be careful of this behavior. Passing
   * [Optional.empty] will clear any existing ID on the model.
   *
   * @param instance Model instance to splice the value into. Because models are immutable, this involves converting the
   * model to a builder, splicing in the value, and then re-building the model. As such, the model
   * returned will be a *different object instance*, but will otherwise be untouched.
   * @param value Value we should splice-into the ID field for the record. It is expected that the generic type of this
   * value will line up with the ID field type, otherwise a [ClassCastException] will be thrown.
   * @param Model Type of model we are splicing an ID value into.
   * @param Value Type of ID value we are splicing into the model.
   * @return Model instance, rebuilt, after splicing in the provided value, at the model's ID-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with IDs at all.
   * @throws ClassCastException If the `Value` generic type does not match the ID field primitive type.
   * @throws MissingAnnotatedField If the provided `instance` is not of the correct type, or has no ID field.
   **/
  public fun <Model: Message, Value> spliceId(instance: Message, value: Optional<Value>): Model {
    @Suppress("UNCHECKED_CAST")
    return spliceIdBuilder<Message.Builder, Value>(instance.toBuilder(), value).build() as Model
  }

  /**
   * Splice the provided value at `val`, into the ID field value for the provided model `builder`. If an ID-
   * annotated property cannot be located, or the model is not of a suitable/type role for use with IDs, an exception is
   * raised (see below for more info).
   *
   *
   * If an existing value exists for the model's ID, **it will be replaced**. In most object-based storage engines
   * this will end up copying the object, rather than mutating an ID. Be careful of this behavior. Passing
   * [Optional.empty] will clear any existing ID on the model.
   *
   * @param builder Model instance builder to splice the value into. The builder provided is *mutated in place*, so
   * it will be an identical object instance to the one provided, but with the ID property filled in.
   * @param value Value we should splice-into the ID field for the record. It is expected that the generic type of this
   * value will line up with the ID field type, otherwise a [ClassCastException] will be thrown.
   * @param Builder Type of model builder we are splicing an ID value into.
   * @param Value Type of ID value we are splicing into the model.
   * @return Model builder, after splicing in the provided value, at the model's ID-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with IDs at all.
   * @throws ClassCastException If the `Value` generic type does not match the ID field primitive type.
   * @throws MissingAnnotatedField If the provided `builder` is not of the correct type, or has no ID field.
   **/
  public fun <Builder: Message.Builder, Value> spliceIdBuilder(
    builder: Message.Builder,
    value: Optional<Value>
  ): Builder {
    // resolve descriptor and field
    require(!(value.isPresent && value.get() is Message)) {
      "Cannot set messages as ID values."
    }
    val descriptor = builder.descriptorForType
    enforceAnyRole(descriptor, DatapointType.OBJECT, DatapointType.OBJECT_KEY)
    val fieldPath: String = idField(descriptor)
      .orElseThrow {
        MissingAnnotatedField(
          descriptor,
          FieldType.ID
        )
      }.path
    return spliceArbitraryField(
      builder,
      builder,
      fieldPath,
      value,
      fieldPath
    )
  }

  /**
   * Splice the provided value at `val`, into the key message value for `instance`. If a key-annotated
   * property cannot be located, or the model is not of a suitable/type role for use with keys, an exception is raised
   * (see below for more info).
   *
   * If an existing value is set for the model's key, **it will be replaced**. In most object-based storage
   * engines this will end up copying the object, rather than mutating a key. Keys are usually immutable for this
   * reason, so use this method with care. Passing [Optional.empty] will clear any existing key message
   * currently affixed to the model `instance`.
   *
   * @param instance Model instance to splice the value into. Because models are immutable, this involves converting the
   * model to a builder, splicing in the value, and then re-building the model. As such, the model
   * returned will be a *different object instance*, but will otherwise be untouched.
   * @param value Value we should splice-into the ID field for the record. It is expected that the generic type of this
   * value will line up with the ID field type, otherwise a [ClassCastException] will be thrown.
   * @param Model Type of model we are splicing an ID value into.
   * @param Key Type of key message we are splicing into the model.
   * @return Model instance, rebuilt, after splicing in the provided value, at the model's ID-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with IDs at all.
   * @throws ClassCastException If the `Value` generic type does not match the ID field primitive type.
   * @throws MissingAnnotatedField If the provided `builder` is not of the correct type, or has no ID field.
   **/
  public fun <Model : Message, Key : Message> spliceKey(instance: Message, value: Optional<Key>): Model {
    @Suppress("UNCHECKED_CAST")
    return spliceKeyBuilder<Message.Builder, Key>(instance.toBuilder(), value).build() as Model
  }

  /**
   * Splice the provided value at `val`, into the key message value for the supplied `builder`. If a
   * key-annotated property cannot be located, or the model is not of a suitable/type role for use with keys, an
   * exception is raised (see below for more info).
   *
   * If an existing value is set for the model's key, **it will be replaced**. In most object-based storage
   * engines this will end up copying the object, rather than mutating a key. Keys are usually immutable for this
   * reason, so use this method with care. Passing [Optional.empty] will clear any existing key message
   * currently affixed to the model `instance`.
   *
   * @param builder Model instance builder to splice the value into. The builder provided is *mutated in place*, so
   * it will be an identical object instance to the one provided, but with the key property filled in.
   * @param value Value we should splice-into the key field for the record. It is expected that the generic type of this
   * value will line up with the key message type, otherwise a [ClassCastException] will be thrown.
   * @param Builder Type of model builder we are splicing a key value into.
   * @param Key Type of key message we are splicing into the model.
   * @return Model builder, after splicing in the provided message, at the model's key-annotated field.
   * @throws InvalidModelType If the specified model is not suitable for use with keys at all.
   * @throws ClassCastException If the `Value` generic type does not match the key field primitive type.
   * @throws MissingAnnotatedField If the provided `builder` is not of the correct type, or has no key field.
   **/
  public fun <Builder : Message.Builder, Key : Message> spliceKeyBuilder(
    builder: Message.Builder,
    value: Optional<Key>
  ): Builder {
    // resolve descriptor and key message field
    val descriptor = builder.descriptorForType
    enforceRole(descriptor, DatapointType.OBJECT)
    val fieldPath: String = keyField(descriptor)
      .orElseThrow {
        MissingAnnotatedField(
          descriptor,
          FieldType.KEY
        )
      }.path
    return spliceArbitraryField(
      builder,
      builder,
      fieldPath,
      value,
      fieldPath
    )
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. This data may also be accessed via a Java stream via the
   * method variants listed below. Variants of this method also allow predicate-based filtering or control of recursion.
   *
   * @see allFields for other variants of this method
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @return Iterable of all fields, recursively, from the descriptor.
   */
  public fun allFields(descriptor: Descriptor): Iterable<FieldPointer> {
    return allFields(descriptor, Optional.empty(), true)
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. For each field encountered, run `predicate` to determine
   * whether to include the field, filtering the returned iterable accordingly. This data may also be accessed via a
   * Java stream via the method variants listed below.
   *
   * @see allFields for other variants of this method
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Iterable of all fields, recursively, from the descriptor, filtered by `predicate`.
   */
  public fun allFields(descriptor: Descriptor, predicate: Optional<Predicate<FieldPointer>>): Iterable<FieldPointer> {
    return allFields(descriptor, predicate, true)
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. For each field encountered, run `predicate` to determine
   * whether to include the field, filtering the returned iterable accordingly. This data may also be accessed via a
   * Java stream via the method variants listed below.
   *
   * @see streamFields for low level field streaming.
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Iterable of all fields, optionally recursively, from the descriptor, filtered by `predicate`.
   */
  public fun allFields(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    recursive: Boolean
  ): Iterable<FieldPointer> {
    return streamFields(
      descriptor,
      predicate,
      recursive
    ).collect(Collectors.toUnmodifiableList())
  }

  /**
   * Crawl all fields, recursively, on the descriptor provided. For each field encountered, run `predicate` to determine
   * whether to include the field, filtering the returned iterable accordingly. This data may also be accessed via a
   * Java stream via the method variants listed below.
   *
   * If a `MESSAGE` field is encountered and the algorithm needs to decide whether to recurse, this variant includes
   * support for the `decider` function. `decider` is invoked to decide whether to recurse for opportunity to do so.
   *
   * @see streamFields for low level field streaming.
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @param decider Function which decides whether to recurse, for each opportunity to do so.
   * @return Iterable of all fields, optionally recursively, from the descriptor, filtered by `predicate`.
   */
  public fun allFields(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    decider: Predicate<FieldPointer>
  ): Iterable<FieldPointer> {
    return streamFields(
      descriptor,
      predicate,
      decider
    ).collect(Collectors.toUnmodifiableList())
  }

  /**
   * Crawl all fields, recursively, on the provided descriptor for a model instance. For each field encountered, run
   * `predicate` to determine whether to include the field, filtering the returned stream of fields accordingly. This
   * method variant runs each operation serially.
   *
   * This method variant does not allow the invoking user to crawl recursively.
   *
   * @see streamFields for low-level field streaming.
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun forEachField(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>
  ): Stream<FieldPointer> {
    Objects.requireNonNull(descriptor)
    Objects.requireNonNull(predicate)
    return streamFieldsRecursive(
      descriptor,
      descriptor,
      predicate,
      { false },
      "",
      false
    )
  }

  /**
   * Crawl all fields, recursively, on the provided descriptor for a model instance. For each field encountered, run
   * `predicate` to determine whether to include the field, filtering the returned stream of fields accordingly. This
   * method variant runs each operation serially.
   *
   * This method variant allows the user to restrict recursive crawling. If recursion is active, a depth-first search
   * is performed, with the `predicate` function invoked for every field encountered during the crawl. If no predicate
   * is provided, the entire set of recursive effective fields is returned from the provided descriptor.
   *
   * @see streamFields for low-level field streaming.
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @param recursive Whether to perform recursion down to sub-messages.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun forEachField(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    recursive: Boolean
  ): Stream<FieldPointer> {
    Objects.requireNonNull(descriptor)
    Objects.requireNonNull(predicate)
    return streamFieldsRecursive(
      descriptor,
      descriptor,
      predicate,
      { recursive },
      "",
      false
    )
  }

  /**
   * Crawl all fields, recursively, on the provided descriptor for a model instance. For each field encountered, run
   * `predicate` to determine whether to include the field, filtering the returned stream of fields accordingly. This
   * method variant runs each operation serially.
   *
   * If a `MESSAGE` field is encountered and the algorithm needs to decide whether to recurse, this variant includes
   * support for the `decider` function. `decider` is invoked to decide whether to recurse for opportunity to do so.
   *
   * This method variant allows the user to restrict recursive crawling. If recursion is active, a depth-first search
   * is performed, with the `predicate` function invoked for every field encountered during the crawl. If no predicate
   * is provided, the entire set of recursive effective fields is returned from the provided descriptor.
   *
   * @see streamFields for low-level field streaming.
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @param decider Function that decides whether to recurse.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun forEachField(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    decider: Predicate<FieldPointer>
  ): Stream<FieldPointer> {
    Objects.requireNonNull(descriptor)
    Objects.requireNonNull(predicate)
    return streamFieldsRecursive(
      descriptor,
      descriptor,
      predicate,
      decider,
      "",
      false
    )
  }

  /**
   * Crawl all fields, recursively, on the descriptor associated with the provided model instance, and return them in
   * a stream.
   *
   * This method crawls recursively by default, but this behavior can be customized via the alternate method variants
   * listed below. Other variants also allow applying a predicate to filter the returned fields.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun <M : Message> streamFields(descriptor: Descriptor): Stream<FieldPointer> {
    return streamFields(descriptor, Optional.empty())
  }

  /**
   * Crawl all fields, recursively, on the descriptor associated with the provided model instance. For each field
   * encountered, run `predicate` to determine whether to include the field, filtering the returned stream of fields
   * accordingly.
   *
   * This method crawls recursively by default, but this behavior can be customized via the alternate method variants
   * listed below.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun streamFields(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>
  ): Stream<FieldPointer> {
    return streamFields(descriptor, predicate, true)
  }

  /**
   * Crawl all fields, recursively, on the descriptor associated with the provided model instance. For each field
   * encountered, run `predicate` to determine whether to include the field, filtering the returned stream of fields
   * accordingly. In this case, `predicate` is required.
   *
   * This method crawls recursively by default, but this behavior can be customized via the alternate method variants
   * listed below.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun streamFields(
    descriptor: Descriptor,
    predicate: Predicate<FieldPointer>
  ): Stream<FieldPointer> {
    return streamFields(descriptor, Optional.of(predicate), true)
  }

  /**
   * Crawl all fields, recursively, on the provided descriptor for a model instance. For each field encountered, run
   * `predicate` to determine whether to include the field, filtering the returned stream of fields accordingly.
   *
   * This method variant allows the user to restrict recursive crawling. If recursion is active, a depth-first search
   * is performed, with the `predicate` function invoked for every field encountered during the crawl. If no predicate
   * is provided, the entire set of recursive effective fields is returned from the provided descriptor.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @param recursive Whether to descend to sub-models recursively.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun streamFields(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    recursive: Boolean
  ): Stream<FieldPointer> {
    Objects.requireNonNull(recursive, "cannot pass `null` for recursive switch")
    return streamFields(
      descriptor,
      predicate
    ) { recursive }
  }

  /**
   * Crawl all fields, recursively, on the provided descriptor for a model instance. For each field encountered, run
   * `predicate` to determine whether to include the field, filtering the returned stream of fields accordingly. By
   * default, all field streaming methods run in parallel.
   *
   * If a `MESSAGE` field is encountered and the algorithm needs to decide whether to recurse, this variant includes
   * support for the `decider` function. `decider` is invoked to decide whether to recurse for opportunity to do so.
   *
   * This method variant allows the user to restrict recursive crawling. If recursion is active, a depth-first search
   * is performed, with the `predicate` function invoked for every field encountered during the crawl. If no predicate
   * is provided, the entire set of recursive effective fields is returned from the provided descriptor.
   *
   * @param descriptor Schema descriptor to crawl model definitions on.
   * @param predicate Filter predicate function, if applicable.
   * @param decider Function that decides whether to recurse.
   * @return Stream of field descriptors, recursively, which match the `predicate`, if provided.
   */
  public fun streamFields(
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    decider: Predicate<FieldPointer>
  ): Stream<FieldPointer> {
    Objects.requireNonNull(descriptor)
    Objects.requireNonNull(predicate)
    return streamFieldsRecursive(
      descriptor,
      descriptor,
      predicate,
      decider,
      "",
      true
    )
  }

  private fun streamFieldsRecursive(
    base: Descriptor,
    descriptor: Descriptor,
    predicate: Optional<Predicate<FieldPointer>>,
    decider: Predicate<FieldPointer>,
    parent: String,
    parallel: Boolean
  ): Stream<FieldPointer> {
    return (if (parallel)
      descriptor.fields.parallelStream()
    else
      descriptor.fields.stream()
    ).flatMap { field: FieldDescriptor ->
      val path = String.format("%s.%s", parent, field.name)
      val pointer = FieldPointer(
        base,
        parent,
        path,
        field
      )
      val branch = Stream.of(pointer)
      if (field.type == FieldDescriptor.Type.MESSAGE && field.messageType.fullName != field.containingType.fullName
        && decider.test(pointer)
      ) {
        return@flatMap Stream.concat(
          branch, streamFieldsRecursive(
            base,
            descriptor.findFieldByNumber(field.number).messageType,
            predicate,
            decider,
            path,
            parallel
          )
        )
      }
      branch
    }.filter { field: FieldPointer ->
      predicate.map { fieldDescriptorPredicate: Predicate<FieldPointer> ->
        fieldDescriptorPredicate.test(
          field
        )
      }.orElse(true)
    }
  }
}
