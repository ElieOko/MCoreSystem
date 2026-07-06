package elieoko.app.mcoresystem.domain.model.room

import androidx.room.*
import elieoko.app.mcoresystem.domain.model.DataSelect
import kotlin.collections.forEach

@Entity(
    tableName = "TCurrency",
    indices = [Index(
        value = ["currency_id"],
        unique = true
    )]
)
data class CurrencyModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "currency_id")
    val id : Int = 0,
    @ColumnInfo(name = "name")
    val name : String = "",
    @ColumnInfo(name = "code")
    val code : String = "",
    @ColumnInfo(name = "symbol")
    val symbol : String = "",
    @ColumnInfo(name = "uuid", defaultValue = "")
    val uuid : String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "updated_at", defaultValue = "")
    val updatedAt : String = ""
){
    fun asDataSelect(item: List<CurrencyModel>?) : List<DataSelect>{
        val listDataSelect = mutableListOf<DataSelect>()
        item?.forEach {
            listDataSelect.add(DataSelect(id = it.id, name = "${it.symbol} (${it.code})"))
        }
        return listDataSelect
    }
}