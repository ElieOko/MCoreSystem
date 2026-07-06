package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel

@Dao
interface ISyncQueueDao {
    @Query("SELECT * FROM TSyncQueue ORDER BY sync_id ASC")
    fun getAll(): List<SyncQueueModel>

    @Query("SELECT COUNT(*) FROM TSyncQueue")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: SyncQueueModel)

    @Query("DELETE FROM TSyncQueue WHERE sync_id = :id")
    fun deleteById(id: Int)

    @Query("DELETE FROM TSyncQueue WHERE entity_uuid = :uuid AND operation = 'UPSERT'")
    fun removeUpsertsFor(uuid: String)
}
