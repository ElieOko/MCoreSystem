package elieoko.app.mcoresystem.domain.repository.room

import androidx.annotation.WorkerThread
import elieoko.app.mcoresystem.domain.interfaces.room.ICategoryDao
import elieoko.app.mcoresystem.domain.interfaces.room.ICurrencyDao
import elieoko.app.mcoresystem.domain.interfaces.room.IOrganismDao
import elieoko.app.mcoresystem.domain.interfaces.room.ITypeCategoryDao
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel

class TypeCategorieRepository(private val dataDao: ITypeCategoryDao) {
    @WorkerThread
    fun allData() : List<TypeCategoryModel> = dataDao.getAll()

    @WorkerThread
    suspend fun insert(data: TypeCategoryModel) {
        dataDao.insertAll(data)
    }

    @WorkerThread
    suspend fun update(data: TypeCategoryModel) {
        dataDao.updateAll(data)
    }
}
