package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.ICurrencyDao
import elieoko.app.mcoresystem.domain.interfaces.room.IOrganismDao
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel

class OrganismRepository(private val dataDao: IOrganismDao) {
    @WorkerThread
    fun allData() : List<OrganismModel> = dataDao.getAll()

    @WorkerThread
    suspend fun insert(data: OrganismModel) {
        dataDao.insertAll(data)
    }

    @WorkerThread
    suspend fun update(data: OrganismModel) {
        dataDao.updateAll(data)
    }
}
