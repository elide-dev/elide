package elide.model

import com.google.protobuf.Message

/**
 * Extends the base [DatabaseAdapter] interface with model query features, based on the concrete `Query` type
 * defined/provided by a given adapter.
 *
 * @param Key Type of key used to uniquely address models.
 * @param Model Message type which this database adapter is handling.
 * @param ReadRecord Intermediate read record.
 * @param WriteRecord Intermediate write record.
 */
public interface QueryableAdapter<Key: Message, Model: Message, ReadRecord, WriteRecord, Query>:
   DatabaseAdapter<Key, Model, ReadRecord, WriteRecord>, QueryableBase<Key, Model, Query> {
   // Nothing yet.
}
