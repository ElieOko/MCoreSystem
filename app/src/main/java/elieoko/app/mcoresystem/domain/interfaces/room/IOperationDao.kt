package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

@Dao
interface IOperationDao {
    @Query("SELECT * FROM TOperation")
    fun getAll(): List<OperationModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg operationModel: OperationModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg operationModel: OperationModel)

    @Delete
    suspend fun delete(operationModel: OperationModel)
}