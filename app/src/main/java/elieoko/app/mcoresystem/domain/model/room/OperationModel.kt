package elieoko.app.mcoresystem.domain.model.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import elieoko.app.mcoresystem.domain.model.DataSelect

@Entity(
    tableName = "TOperation",
    indices = [Index(
        value = ["operation_id"],
        unique = true
    )]
)
data class OperationModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "operation_id")
    val id : Int = 0,
    @ColumnInfo(name = "organism_id")
    val organismId : Int,
    @ColumnInfo(name = "category_id")
    val categoryId : Int,
    @ColumnInfo(name = "user_id")
    val userId : Int,
    @ColumnInfo(name = "payment_method_id")
    val paymentMethodId : Int,
    @ColumnInfo(name = "currency_id")
    val currencyId : Int,
    @ColumnInfo(name = "amount")
    val amount : Double = 0.0,
    @ColumnInfo(name = "task_name")
    val taskName : String = "",
    @ColumnInfo(name = "description")
    val description : String = "",
    @ColumnInfo(name = "created_on")
    val createdOn : String,
    @ColumnInfo(name = "is_active")
    val isActive : Boolean = true,
){
    fun asDataSelect(item: List<OperationModel>?) : List<DataSelect>{
        val listDataSelect = mutableListOf<DataSelect>()
        item?.forEach {
            listDataSelect.add(DataSelect(id = it.id, name = it.taskName))
        }
        return listDataSelect
    }
}