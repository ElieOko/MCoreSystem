package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.interfaces.room.ITypeCategoryDao
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel
import elieoko.app.mcoresystem.domain.util.TimeUtil

class TypeCategorieRepository(
    private val dataDao: ITypeCategoryDao,
    private val tracker: ChangeTracker? = null
) {
    @WorkerThread
    fun allData() : List<TypeCategoryModel> = dataDao.getAll()

    @WorkerThread
    suspend fun insert(data: TypeCategoryModel) {
        val stamped = data.copy(updatedAt = TimeUtil.nowIso())
        dataDao.insertAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_TYPE_CATEGORY, stamped.uuid)
    }

    @WorkerThread
    suspend fun update(data: TypeCategoryModel) {
        val stamped = data.copy(updatedAt = TimeUtil.nowIso())
        dataDao.updateAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_TYPE_CATEGORY, stamped.uuid)
    }

    @WorkerThread
    suspend fun delete(data: TypeCategoryModel) {
        dataDao.delete(data)
        tracker?.recordDelete(SyncQueueModel.TYPE_TYPE_CATEGORY, data.uuid)
    }
}
