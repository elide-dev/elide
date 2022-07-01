package elide.model

import com.google.protobuf.Message


/**
 * Extends the standard [PersistenceDriver] with intermediate types [ReadRecord] and [WriteRecord], which all
 * sub-classes are expected to define for type-checking of fetch and write types.
 */
public interface DatabaseDriver<Key: Message, Model: Message, ReadRecord, WriteRecord>: PersistenceDriver<Key, Model> {
  // Nothing at this time.
}
