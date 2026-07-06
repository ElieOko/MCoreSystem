package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

@Dao
interface ICategoryDao {
    @Query("SELECT * FROM TCategory")
    fun getAll(): List<CategoryModel>

    @Query("SELECT * FROM TCategory WHERE uuid = :uuid LIMIT 1")
    fun findByUuid(uuid: String): CategoryModel?

    @Query("SELECT * FROM TCategory WHERE category_id = :id LIMIT 1")
    fun findById(id: Int): CategoryModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg categoryModel: CategoryModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg categoryModel: CategoryModel)

    @Delete
    suspend fun delete(categoryModel: CategoryModel)
}