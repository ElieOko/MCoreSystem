package elieoko.app.mcoresystem.domain.model.room

import androidx.room.*

@Entity(
    tableName = "TOrganism",
    indices = [Index(
        value = ["organism_id"],
        unique = true
    )]
)
data class OrganismModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "organism_id") val id: Int,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "uuid", defaultValue = "")
    val uuid: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "updated_at", defaultValue = "")
    val updatedAt: String = ""
)