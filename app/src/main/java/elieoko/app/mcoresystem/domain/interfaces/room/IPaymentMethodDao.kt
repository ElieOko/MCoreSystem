package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

@Dao
interface IPaymentMethodDao {
    @Query("SELECT * FROM TPaymentMethod")
    fun getAll(): List<PaymentMethodModel>

    @Query("SELECT * FROM TPaymentMethod WHERE uuid = :uuid LIMIT 1")
    fun findByUuid(uuid: String): PaymentMethodModel?

    @Query("SELECT * FROM TPaymentMethod WHERE payment_method_id = :id LIMIT 1")
    fun findById(id: Int): PaymentMethodModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg paymentMethods: PaymentMethodModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg paymentMethods: PaymentMethodModel)

    @Delete
    suspend fun delete(paymentMethod: PaymentMethodModel)
}