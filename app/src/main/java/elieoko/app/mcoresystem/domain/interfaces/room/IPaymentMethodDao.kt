package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

@Dao
interface IPaymentMethodDao {
    @Query("SELECT * FROM TPaymentMethod")
    fun getAll(): List<PaymentMethodModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg paymentMethods: PaymentMethodModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg paymentMethods: PaymentMethodModel)

    @Delete
    suspend fun delete(paymentMethod: PaymentMethodModel)
}