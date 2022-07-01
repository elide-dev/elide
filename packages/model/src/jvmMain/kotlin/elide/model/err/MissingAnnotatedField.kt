package elide.model.err

import com.google.protobuf.Descriptors
import tools.elide.model.FieldType

/** Specifies that a model was missing a required annotated-field for a given operation.  */
@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
public class MissingAnnotatedField internal constructor(
  /** Specifies the descriptor that violated the required field constraint.  */
  public val violatingSchema: Descriptors.Descriptor,
  public val type: FieldType
) :
  PersistenceException(
    "Model type '${violatingSchema.name}' failed to be processed, because it is missing the annotated field " +
    "'${type.name}', which was required for the requested operation."
  ) {
  /** Field type that was required but not found.  */
  public val requiredField: FieldType = type
}
