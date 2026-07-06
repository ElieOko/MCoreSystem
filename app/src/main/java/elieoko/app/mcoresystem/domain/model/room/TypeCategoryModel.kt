package elieoko.app.mcoresystem.domain.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import elieoko.app.mcoresystem.domain.model.DataSelect

@Entity(
    tableName = "TTypeCategory",
    indices = [Index(
        value = ["type_category_id"],
        unique = true
    )]
)
data class TypeCategoryModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "type_category_id")
    val id : Int = 0,
    @ColumnInfo(name = "organism_id")
    val organismId : Int,
    @ColumnInfo(name = "name")
    val name : String = "",
    @ColumnInfo(name = "description")
    val description : String = "",
    @ColumnInfo(name = "is_active")
    val isActive : Boolean = true,
    @ColumnInfo(name = "uuid", defaultValue = "")
    val uuid : String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "updated_at", defaultValue = "")
    val updatedAt : String = ""
){
    fun asDataSelect(item: List<TypeCategoryModel>?) : List<DataSelect>{
        val listDataSelect = mutableListOf<DataSelect>()
        item?.forEach {
            listDataSelect.add(DataSelect(id = it.id, name = it.name))
        }
        return listDataSelect
    }
}