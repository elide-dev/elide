package elide.model

import com.google.protobuf.Message

/**
 * Extends the standard [ModelAdapter] interface with rich persistence features.
 *
 * @param Key Type of key used to uniquely address models.
 * @param Model Message type which this database adapter is handling.
 * @param ReadRecord Intermediate read record.
 * @param WriteRecord Intermediate write record.
 */
public interface DatabaseAdapter<Key: Message, Model: Message, ReadRecord, WriteRecord>: ModelAdapter<Key, Model> {
  /**
   * Return the lower-level [DatabaseDriver] powering this adapter. The driver is responsible for communicating
   * with the actual database or storage service, either via local stubs/emulators or a production API.
   *
   * @return Database driver instance currently in use by this model adapter.
   */
  override fun engine(): DatabaseDriver<Key, Model, ReadRecord, WriteRecord>

  /** @inheritDoc  */
  override fun generateKey(instance: Message): Key {
    return engine().generateKey(instance)
  }
}
