package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.interfaces.room.IOperationDao
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import elieoko.app.mcoresystem.domain.util.TimeUtil
import kotlinx.coroutines.flow.Flow

class OperationRepository(
    private val dataDao: IOperationDao,
    private val tracker: ChangeTracker? = null
) {

    @WorkerThread
    fun allOperation(userId : Int) : Flow<List<OperationRelation>> = dataDao.getAll(userId)

    @WorkerThread
    fun getDetailOperation(operationId : Int) : Flow<OperationRelation> = dataDao.getDetailOperation(operationId)

    @WorkerThread
    fun allOperationDay(dateCurrent: String, currencyId : Int, userId : Int) : Int? = dataDao.getOperationToDay(dateCurrent, currencyId, userId)

    @WorkerThread
    fun allOperationDayCDF(dateCurrent: String, currencyId : Int, userId : Int) : Int? = dataDao.getOperationToDayCDF(dateCurrent, currencyId, userId)

    @WorkerThread
    fun pendingOperations() : List<OperationRelation> = dataDao.getPendingOperations()

    @WorkerThread
    suspend fun updateStatus(operationId: Int, status: String) {
        dataDao.updateStatus(operationId, status, TimeUtil.nowIso())
        dataDao.findById(operationId)?.let {
            tracker?.recordUpsert(SyncQueueModel.TYPE_OPERATION, it.uuid)
        }
    }

    @WorkerThread
    suspend fun insert(data: OperationModel): Long {
        val stamped = data.copy(updatedAt = TimeUtil.nowIso())
        val row = dataDao.insertAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_OPERATION, stamped.uuid)
        return row
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun update(data: OperationModel) {
        val stamped = data.copy(updatedAt = TimeUtil.nowIso())
        dataDao.updateAll(stamped)
        tracker?.recordUpsert(SyncQueueModel.TYPE_OPERATION, stamped.uuid)
    }

    @WorkerThread
    suspend fun delete(data: OperationModel) {
        dataDao.delete(data)
        tracker?.recordDelete(SyncQueueModel.TYPE_OPERATION, data.uuid)
    }
}
