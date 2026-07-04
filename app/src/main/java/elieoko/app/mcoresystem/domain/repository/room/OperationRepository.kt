package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.ICurrencyDao
import elieoko.app.mcoresystem.domain.interfaces.room.IOperationDao
import elieoko.app.mcoresystem.domain.interfaces.room.IOrganismDao
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel

class OperationRepository(private val dataDao: IOperationDao) {
    @WorkerThread
    fun allData() : List<OperationModel> = dataDao.getAll()

    @WorkerThread
    suspend fun insert(data: OperationModel) {
        dataDao.insertAll(data)
    }

    @WorkerThread
    suspend fun update(data: OperationModel) {
        dataDao.updateAll(data)
    }
}
