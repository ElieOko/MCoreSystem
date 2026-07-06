package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.interfaces.room.ICategoryDao
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.util.TimeUtil

class CategorieRepository(
    private val dataDao: ICategoryDao,
    private val tracker: ChangeTracker? = null
) {
    @WorkerThread
    fun allData() : List<CategoryModel> = dataDao.getAll()

    @WorkerThread
    suspend fun insert(data: CategoryModel) {
        val stamped = data.copy(updatedAt = TimeUtil.nowIso())
        dataDao.insertAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_CATEGORY, stamped.uuid)
    }

    @WorkerThread
    suspend fun update(data: CategoryModel) {
        val stamped = data.copy(updatedAt = TimeUtil.nowIso())
        dataDao.updateAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_CATEGORY, stamped.uuid)
    }

    @WorkerThread
    suspend fun delete(data: CategoryModel) {
        dataDao.delete(data)
        tracker?.recordDelete(SyncQueueModel.TYPE_CATEGORY, data.uuid)
    }
}
