package elieoko.app.mcoresystem.domain.interfaces.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import elieoko.app.mcoresystem.domain.model.room.UserModel
import kotlinx.coroutines.flow.Flow

@Dao
interface IUserDao {
    @Query("SELECT * FROM TUser")
    fun getAll(): Flow<List<UserModel>>

    @Query("SELECT * FROM TUser WHERE user_id IN (:userId)")
    fun loadAllById(userId: Int): List<UserModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: UserModel)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateAll(vararg users: UserModel)

    @Delete
    fun delete(user: UserModel)
}