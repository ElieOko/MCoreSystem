package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.ICategoryDao
import elieoko.app.mcoresystem.domain.interfaces.room.ICurrencyDao
import elieoko.app.mcoresystem.domain.interfaces.room.IOrganismDao
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel

class CategorieRepository(private val dataDao: ICategoryDao) {
    @WorkerThread
    fun allData() : List<CategoryModel> = dataDao.getAll()

    @WorkerThread
    suspend fun insert(data: CategoryModel) {
        dataDao.insertAll(data)
    }

    @WorkerThread
    suspend fun update(data: CategoryModel) {
        dataDao.updateAll(data)
    }

    @WorkerThread
    suspend fun delete(data: CategoryModel) {
        dataDao.delete(data)
    }
}
