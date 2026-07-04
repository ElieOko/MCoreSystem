package elieoko.app.mcoresystem.domain.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import elieoko.app.mcoresystem.domain.model.DataSelect

@Entity(
    tableName = "TCategory",
    indices = [Index(
        value = ["category_id"],
        unique = true
    )]
)
data class CategoryModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "category_id")
    val id : Int = 0,
    @ColumnInfo(name = "organism_id")
    val organismId : Int,
    @ColumnInfo(name = "type_category_id")
    val typeCategoryId : Int,
    @ColumnInfo(name = "name")
    val name : String = "",
    @ColumnInfo(name = "description")
    val description : String = ""
){
    fun asDataSelect(item: List<CategoryModel>?) : List<DataSelect>{
        val listDataSelect = mutableListOf<DataSelect>()
        item?.forEach {
            listDataSelect.add(DataSelect(id = it.id, name = it.name))
        }
        return listDataSelect
    }
}