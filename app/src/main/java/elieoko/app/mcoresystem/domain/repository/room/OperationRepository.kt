package elieoko.app.mcoresystem.domain.repository.room

import android.util.Log
import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.IOperationDao
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import kotlinx.coroutines.flow.Flow

class OperationRepository(private val dataDao: IOperationDao) {

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
    suspend fun updateStatus(operationId: Int, status: String) = dataDao.updateStatus(operationId, status)

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(data: OperationModel): Long {
        Log.e("repository =>","$data")
        return dataDao.insertAll(data)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun update(data: OperationModel) {
        dataDao.updateAll(data)
    }

    @WorkerThread
    suspend fun delete(data: OperationModel) {
        dataDao.delete(data)
    }
}
