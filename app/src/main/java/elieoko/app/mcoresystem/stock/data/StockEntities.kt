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
    val createdOn: String,
    @ColumnInfo(name = "organism_id", defaultValue = "0")
    val organismId: Long = 0,
    @ColumnInfo(name = "uuid", defaultValue = "")
    val uuid: String = ""
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
    val createdOn: String,
    @ColumnInfo(name = "organism_id", defaultValue = "0")
    val organismId: Long = 0,
    @ColumnInfo(name = "uuid", defaultValue = "")
    val uuid: String = ""
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
    val quantity: Double,
    @ColumnInfo(name = "organism_id", defaultValue = "0")
    val organismId: Long = 0
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
    val createdOn: String,
    @ColumnInfo(name = "organism_id", defaultValue = "0")
    val organismId: Long = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Long? = null
)

enum class StockRole { OWNER, LIVREUR }

enum class OrderStatus(val label: String) {
    BROUILLON("Brouillon"),
    ASSIGNEE("Assignée"),
    EN_LIVRAISON("En livraison"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    companion object {
        fun from(raw: String): OrderStatus = entries.firstOrNull { it.name == raw } ?: BROUILLON
    }
}

@Entity(tableName = "stock_organism")
data class StockOrganism(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "organism_id")
    val id: Long = 0,
    @ColumnInfo(name = "uuid")
    val uuid: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_on")
    val createdOn: String
)

@Entity(tableName = "stock_account")
data class StockAccount(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "account_id")
    val id: Long = 0,
    @ColumnInfo(name = "organism_id")
    val organismId: Long,
    @ColumnInfo(name = "livreur_id")
    val livreurId: Long? = null,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "created_on")
    val createdOn: String
)

@Entity(tableName = "stock_order")
data class StockOrder(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "order_id")
    val id: Long = 0,
    @ColumnInfo(name = "organism_id")
    val organismId: Long,
    @ColumnInfo(name = "livreur_id")
    val livreurId: Long? = null,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "note")
    val note: String = "",
    @ColumnInfo(name = "created_on")
    val createdOn: String
)

@Entity(tableName = "stock_order_line")
data class StockOrderLine(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "line_id")
    val id: Long = 0,
    @ColumnInfo(name = "order_id")
    val orderId: Long,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    @ColumnInfo(name = "quantity_ordered")
    val quantityOrdered: Double,
    @ColumnInfo(name = "quantity_delivered")
    val quantityDelivered: Double = 0.0
)

data class DeliveryLine(
    val productId: Long,
    val quantity: Double
)
