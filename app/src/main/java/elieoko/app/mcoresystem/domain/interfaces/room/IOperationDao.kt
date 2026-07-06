package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.OperationModel
import elieoko.app.mcoresystem.domain.model.room.relation.OperationRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface IOperationDao {
    @Transaction
    @Query("SELECT * FROM TOperation WHERE user_id LIKE :userId ORDER BY operation_id DESC")
    fun getAll(userId : Int): Flow<List<OperationRelation>>

    @Transaction
    @Query("SELECT * FROM TOperation WHERE status != 'CLOTURE' ORDER BY operation_id DESC")
    fun getPendingOperations(): List<OperationRelation>

    @Query("UPDATE TOperation SET status = :status WHERE operation_id = :operationId")
    suspend fun updateStatus(operationId: Int, status: String)

    @Transaction
    @Query("SELECT * FROM TOperation WHERE operation_id LIKE :operationId")
    fun getDetailOperation(operationId : Int): Flow<OperationRelation>

    @Query("SELECT SUM(amount) FROM TOperation WHERE created_on LIKE :dateCurrent AND currency_id LIKE :currencyId AND user_id LIKE :userId")
    fun getOperationToDay(dateCurrent: String, currencyId: Int, userId: Int): Int?

    @Query("SELECT SUM(amount) FROM TOperation WHERE created_on LIKE :dateCurrent AND currency_id LIKE :currencyId AND user_id LIKE :userId")
    fun getOperationToDayCDF(dateCurrent: String, currencyId: Int, userId: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: OperationModel):Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAll(vararg operations: OperationModel)

    @Delete
    suspend fun delete(operationModel: OperationModel)


}