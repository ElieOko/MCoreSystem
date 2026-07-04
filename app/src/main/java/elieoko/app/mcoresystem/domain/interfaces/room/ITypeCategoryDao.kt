package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel
import elieoko.app.mcoresystem.domain.model.room.TypeCategoryModel

@Dao
interface ITypeCategoryDao {
    @Query("SELECT * FROM TTypeCategory")
    fun getAll(): List<TypeCategoryModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg typeCategoryModel: TypeCategoryModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg typeCategoryModel: TypeCategoryModel)

    @Delete
    suspend fun delete(typeCategoryModel: TypeCategoryModel)
}