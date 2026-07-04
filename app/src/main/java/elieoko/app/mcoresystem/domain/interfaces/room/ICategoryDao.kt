package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

@Dao
interface ICategoryDao {
    @Query("SELECT * FROM TCategory")
    fun getAll(): List<CategoryModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg categoryModel: CategoryModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg categoryModel: CategoryModel)

    @Delete
    suspend fun delete(categoryModel: CategoryModel)
}