package elide.model.err

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import tools.elide.model.DatapointType

/**
 * Specifies an error, wherein a user has requested a data adapter or some other database object, for a model which is
 * not usable with data storage systems (via annotations).
 */
@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
public class InvalidModelType internal constructor(
  /** Message that violated some type expectation.  */
  public val violatingSchema: Descriptors.Descriptor,
  expectedTypes: Set<DatapointType>
) : PersistenceException(
  "Invalid model type: '${violatingSchema.name}' is not one of the allowed types " +
  "'${expectedTypes.joinToString(", ")}'."
) {
  /** Set of allowed types that the message didn't match.  */
  private val datapointTypes: Set<DatapointType>

  init {
    datapointTypes = expectedTypes
  }

  // -- Getters -- //

  /** @return Allowed types which the violating message did not match. */
  public fun getDatapointTypes(): Set<DatapointType> {
    return datapointTypes
  }

  public companion object {
    /**
     * Factory to create a new `InvalidModelType` exception directly from a model descriptor.
     *
     * @param type Type of model to create this exception for.
     * @param expectedTypes Available types, any one of which it can conform to.
     * @return Created `InvalidModelType` exception.
     */
    @JvmStatic
    public fun from(type: Descriptors.Descriptor, expectedTypes: Set<DatapointType>): InvalidModelType {
      return InvalidModelType(type, expectedTypes)
    }

    /**
     * Factory to create a new `InvalidModelType` exception from a model instance.
     *
     * @param type Type of model to create this exception for.
     * @param expectedTypes Available types, any one of which it can conform to.
     * @return Created `InvalidModelType` exception.
     */
    @JvmStatic
    public fun from(type: Message, expectedTypes: Set<DatapointType>): InvalidModelType {
      return from(type.descriptorForType, expectedTypes)
    }
  }
}
