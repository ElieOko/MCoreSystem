package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import elieoko.app.mcoresystem.domain.model.room.CurrencyModel

@Dao
interface ICurrencyDao {
    @Query("SELECT * FROM TCurrency")
    fun getAll(): List<CurrencyModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg currencies: CurrencyModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg currencies: CurrencyModel)

    @Delete
    suspend fun delete(currency: CurrencyModel)
}