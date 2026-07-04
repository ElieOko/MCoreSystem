package elieoko.app.mcoresystem.domain.model.room

import androidx.room.*

@Entity(
    tableName = "TUser",
    indices = [Index(
        value = ["user_id"],
        unique = true
    )]
)
data class UserModel(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "user_id") val id: Int,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "organism_id") val organismId: Int,
)