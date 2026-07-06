package elieoko.app.mcoresystem.domain.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import elieoko.app.mcoresystem.domain.model.DataSelect

@Entity(
    tableName = "TPaymentMethod",
    indices = [Index(
        value = ["payment_method_id"],
        unique = true
    )]
)
data class PaymentMethodModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "payment_method_id") val id : Int = 0,
    @ColumnInfo(name = "name") val name : String = "",
    @ColumnInfo(name = "uuid", defaultValue = "")
    val uuid : String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "updated_at", defaultValue = "")
    val updatedAt : String = ""
){
    fun asDataSelect(item: List<PaymentMethodModel>?) : List<DataSelect>{
        val listDataSelect = mutableListOf<DataSelect>()
        item?.forEach {
            listDataSelect.add(DataSelect(id = it.id, name = it.name))
        }
        return listDataSelect
    }
}