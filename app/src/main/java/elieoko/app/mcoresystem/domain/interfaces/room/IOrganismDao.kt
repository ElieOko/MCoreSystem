package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.room.CategoryModel
import elieoko.app.mcoresystem.domain.model.room.OrganismModel
import elieoko.app.mcoresystem.domain.model.room.PaymentMethodModel

@Dao
interface IOrganismDao {
    @Query("SELECT * FROM TOrganism")
    fun getAll(): List<OrganismModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg organismModel: OrganismModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(vararg organismModel: OrganismModel)

    @Delete
    suspend fun delete(organismModel: OrganismModel)
}