package elieoko.app.mcoresystem.stock.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stock_product")
data class StockProduct(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "product_id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "unit")
    val unit: String,
    @ColumnInfo(name = "central_quantity")
    val centralQuantity: Double = 0.0,
    @ColumnInfo(name = "created_on")
    val createdOn: String
)

@Entity(tableName = "stock_livreur")
data class StockLivreur(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "livreur_id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "phone")
    val phone: String = "",
    @ColumnInfo(name = "created_on")
    val createdOn: String
)

@Entity(
    tableName = "mini_stock",
    indices = [Index(value = ["livreur_id", "product_id"], unique = true)]
)
data class MiniStockLine(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "mini_stock_id")
    val id: Long = 0,
    @ColumnInfo(name = "livreur_id")
    val livreurId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "quantity")
    val quantity: Double
)

enum class MovementType(val label: String) {
    ENTREE("Entrée dépôt"),
    RETRAIT("Retrait dépôt"),
    DOTATION("Mini-stock donné"),
    RETOUR("Retour dépôt"),
    SORTIE("Sortie livreur");

    companion object {
        fun from(raw: String): MovementType = entries.firstOrNull { it.name == raw } ?: ENTREE
    }
}

@Entity(tableName = "stock_movement")
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "movement_id")
    val id: Long = 0,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "product_name")
    val productName: String,
    @ColumnInfo(name = "livreur_id")
    val livreurId: Long? = null,
    @ColumnInfo(name = "livreur_name")
    val livreurName: String? = null,
    @ColumnInfo(name = "quantity")
    val quantity: Double,
    @ColumnInfo(name = "note")
    val note: String = "",
    @ColumnInfo(name = "created_on")
    val createdOn: String
)
