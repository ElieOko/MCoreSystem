package elieoko.app.mcoresystem.stock.data

import android.util.Log
import elieoko.app.mcoresystem.data.remote.SupabaseProvider
import elieoko.app.mcoresystem.domain.util.TimeUtil
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Pousse et récupère le stock de l'organisation dans Supabase.
 * Room reste la source lue par l'écran. Sans réseau, les écritures restent locales.
 */
class StockSync(private val database: StockDatabase) {
    private val dao = database.stockDao()

    suspend fun sync(organismUuid: String): String? {
        if (organismUuid.isBlank()) return "Organisation inconnue"
        val client = SupabaseProvider.client ?: return null
        val organism = dao.organismByUuid(organismUuid) ?: return null
        return try {
            stampMissing(organism.id)
            push(organism)
            pull(organism)
            null
        } catch (error: Exception) {
            Log.e(TAG, "Sync stock impossible", error)
            error.message ?: "Synchronisation du stock impossible"
        }
    }

    private suspend fun stampMissing(organismId: Long) {
        val now = TimeUtil.nowIso()
        dao.productsOf(organismId).forEach { product ->
            if (product.uuid.isBlank() || product.updatedAt.isBlank()) {
                dao.updateProduct(
                    product.copy(
                        uuid = product.uuid.ifBlank { UUID.randomUUID().toString() },
                        updatedAt = product.updatedAt.ifBlank { now }
                    )
                )
            }
        }
        dao.livreursOf(organismId).forEach { livreur ->
            if (livreur.uuid.isBlank() || livreur.updatedAt.isBlank()) {
                dao.updateLivreur(
                    livreur.copy(
                        uuid = livreur.uuid.ifBlank { UUID.randomUUID().toString() },
                        updatedAt = livreur.updatedAt.ifBlank { now }
                    )
                )
            }
        }
        dao.minisOf(organismId).forEach { line ->
            if (line.uuid.isBlank() || line.updatedAt.isBlank()) {
                dao.updateMini(
                    line.copy(
                        uuid = line.uuid.ifBlank { UUID.randomUUID().toString() },
                        updatedAt = line.updatedAt.ifBlank { now }
                    )
                )
            }
        }
        dao.movementsOf(organismId).forEach { movement ->
            if (movement.uuid.isBlank() || movement.updatedAt.isBlank()) {
                dao.updateMovement(
                    movement.copy(
                        uuid = movement.uuid.ifBlank { UUID.randomUUID().toString() },
                        updatedAt = movement.updatedAt.ifBlank { now }
                    )
                )
            }
        }
        dao.ordersOf(organismId).forEach { order ->
            if (order.uuid.isBlank() || order.updatedAt.isBlank()) {
                dao.updateOrder(
                    order.copy(
                        uuid = order.uuid.ifBlank { UUID.randomUUID().toString() },
                        updatedAt = order.updatedAt.ifBlank { now }
                    )
                )
            }
            dao.linesForOrder(order.id).forEach { line ->
                if (line.uuid.isBlank() || line.updatedAt.isBlank()) {
                    dao.updateOrderLine(
                        line.copy(
                            uuid = line.uuid.ifBlank { UUID.randomUUID().toString() },
                            updatedAt = line.updatedAt.ifBlank { now }
                        )
                    )
                }
            }
        }
    }

    private suspend fun push(organism: StockOrganism) {
        val client = SupabaseProvider.client ?: return
        val products = dao.productsOf(organism.id)
        if (products.isNotEmpty()) {
            client.from(PRODUCTS).upsert(products.map { it.toRemote(organism.uuid) }) { onConflict = "uuid" }
        }
        val livreurs = dao.livreursOf(organism.id)
        if (livreurs.isNotEmpty()) {
            client.from(LIVREURS).upsert(livreurs.map { it.toRemote(organism.uuid) }) { onConflict = "uuid" }
        }
        val productUuid = products.associate { it.id to it.uuid }
        val livreurUuid = livreurs.associate { it.id to it.uuid }
        val minis = dao.minisOf(organism.id).mapNotNull { line ->
            val p = productUuid[line.productId] ?: return@mapNotNull null
            val l = livreurUuid[line.livreurId] ?: return@mapNotNull null
            line.toRemote(organism.uuid, l, p)
        }
        if (minis.isNotEmpty()) client.from(MINIS).upsert(minis) { onConflict = "uuid" }
        val orders = dao.ordersOf(organism.id)
        if (orders.isNotEmpty()) {
            client.from(ORDERS).upsert(orders.map { it.toRemote(organism.uuid, livreurUuid[it.livreurId]) }) {
                onConflict = "uuid"
            }
        }
        val lines = orders.flatMap { order ->
            dao.linesForOrder(order.id).mapNotNull { line ->
                val product = productUuid[line.productId] ?: return@mapNotNull null
                line.toRemote(order.uuid, product)
            }
        }
        if (lines.isNotEmpty()) client.from(LINES).upsert(lines) { onConflict = "uuid" }
        val movements = dao.movementsOf(organism.id).map { movement ->
            movement.toRemote(
                organism.uuid,
                productUuid[movement.productId],
                movement.livreurId?.let { livreurUuid[it] },
                movement.orderId?.let { id -> orders.firstOrNull { it.id == id }?.uuid }
            )
        }
        if (movements.isNotEmpty()) client.from(MOVEMENTS).upsert(movements) { onConflict = "uuid" }
    }

    private suspend fun pull(organism: StockOrganism) {
        val client = SupabaseProvider.client ?: return
        val remoteProducts = client.from(PRODUCTS).select(Columns.ALL) {
            filter { eq("organism_uuid", organism.uuid) }
        }.decodeList<RemoteStockProduct>()
        remoteProducts.forEach { remote ->
            val local = dao.productByUuid(remote.uuid)
            if (local == null) {
                dao.insertProduct(remote.toLocal(organism.id))
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                dao.updateProduct(
                    local.copy(
                        name = remote.name,
                        unit = remote.unit,
                        centralQuantity = remote.centralQuantity,
                        unitPrice = remote.unitPrice,
                        updatedAt = remote.updatedAt
                    )
                )
            }
        }
        val remoteLivreurs = client.from(LIVREURS).select(Columns.ALL) {
            filter { eq("organism_uuid", organism.uuid) }
        }.decodeList<RemoteStockLivreur>()
        remoteLivreurs.forEach { remote ->
            val local = dao.livreurByUuid(remote.uuid)
            if (local == null) {
                dao.insertLivreur(remote.toLocal(organism.id))
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                dao.updateLivreur(
                    local.copy(
                        name = remote.name,
                        phone = remote.phone,
                        userUuid = remote.userUuid.orEmpty(),
                        updatedAt = remote.updatedAt
                    )
                )
            }
        }
        val products = dao.productsOf(organism.id).associateBy { it.uuid }
        val livreurs = dao.livreursOf(organism.id).associateBy { it.uuid }
        val remoteMinis = client.from(MINIS).select(Columns.ALL) {
            filter { eq("organism_uuid", organism.uuid) }
        }.decodeList<RemoteMiniStock>()
        remoteMinis.forEach { remote ->
            val product = products[remote.productUuid] ?: return@forEach
            val livreur = livreurs[remote.livreurUuid] ?: return@forEach
            val local = dao.miniByUuid(remote.uuid) ?: dao.getMini(livreur.id, product.id)
            if (local == null) {
                dao.insertMini(remote.toLocal(organism.id, livreur.id, product.id))
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                dao.updateMini(local.copy(quantity = remote.quantity, uuid = remote.uuid, updatedAt = remote.updatedAt))
            }
        }
        val remoteOrders = client.from(ORDERS).select(Columns.ALL) {
            filter { eq("organism_uuid", organism.uuid) }
        }.decodeList<RemoteStockOrder>()
        remoteOrders.forEach { remote ->
            val livreurId = remote.livreurUuid?.let { livreurs[it]?.id }
            val local = dao.orderByUuid(remote.uuid)
            if (local == null) {
                dao.insertOrder(remote.toLocal(organism.id, livreurId))
            } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                dao.updateOrder(
                    local.copy(
                        customerName = remote.customerName,
                        status = remote.status,
                        note = remote.note,
                        livreurId = livreurId,
                        updatedAt = remote.updatedAt
                    )
                )
            }
        }
        val orders = dao.ordersOf(organism.id).associateBy { it.uuid }
        val orderIds = orders.values.map { it.uuid }
        if (orderIds.isNotEmpty()) {
            val remoteLines = client.from(LINES).select(Columns.ALL) {
                filter { isIn("order_uuid", orderIds) }
            }.decodeList<RemoteStockOrderLine>()
            remoteLines.forEach { remote ->
                val order = orders[remote.orderUuid] ?: return@forEach
                val product = products[remote.productUuid] ?: return@forEach
                val local = dao.orderLineByUuid(remote.uuid)
                if (local == null) {
                    dao.insertOrderLine(remote.toLocal(order.id, product.id))
                } else if (TimeUtil.isNewer(remote.updatedAt, local.updatedAt)) {
                    dao.updateOrderLine(
                        local.copy(
                            quantityOrdered = remote.quantityOrdered,
                            quantityDelivered = remote.quantityDelivered,
                            updatedAt = remote.updatedAt
                        )
                    )
                }
            }
        }
        val remoteMoves = client.from(MOVEMENTS).select(Columns.ALL) {
            filter { eq("organism_uuid", organism.uuid) }
        }.decodeList<RemoteStockMovement>()
        remoteMoves.forEach { remote ->
            if (dao.movementByUuid(remote.uuid) != null) return@forEach
            val product = remote.productUuid?.let { products[it] } ?: return@forEach
            val livreur = remote.livreurUuid?.let { livreurs[it] }
            val order = remote.orderUuid?.let { orders[it] }
            dao.insertMovement(remote.toLocal(organism.id, product.id, livreur?.id, order?.id))
        }
    }

    companion object {
        private const val TAG = "StockSync"
        const val PRODUCTS = "stock_products"
        const val LIVREURS = "stock_livreurs"
        const val MINIS = "mini_stocks"
        const val ORDERS = "stock_orders"
        const val LINES = "stock_order_lines"
        const val MOVEMENTS = "stock_movements"
    }
}

@Serializable
data class RemoteStockProduct(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val name: String = "",
    val unit: String = "pièce",
    @SerialName("central_quantity") val centralQuantity: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteStockLivreur(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val name: String = "",
    val phone: String = "",
    @SerialName("user_uuid") val userUuid: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteMiniStock(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    @SerialName("livreur_uuid") val livreurUuid: String,
    @SerialName("product_uuid") val productUuid: String,
    val quantity: Double = 0.0,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteStockOrder(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    @SerialName("livreur_uuid") val livreurUuid: String? = null,
    @SerialName("customer_name") val customerName: String = "",
    val status: String = "BROUILLON",
    val note: String = "",
    @SerialName("created_on") val createdOn: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteStockOrderLine(
    val uuid: String,
    @SerialName("order_uuid") val orderUuid: String,
    @SerialName("product_uuid") val productUuid: String,
    @SerialName("quantity_ordered") val quantityOrdered: Double = 0.0,
    @SerialName("quantity_delivered") val quantityDelivered: Double = 0.0,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteStockMovement(
    val uuid: String,
    @SerialName("organism_uuid") val organismUuid: String,
    val type: String,
    @SerialName("product_uuid") val productUuid: String? = null,
    @SerialName("product_name") val productName: String = "",
    @SerialName("livreur_uuid") val livreurUuid: String? = null,
    @SerialName("livreur_name") val livreurName: String? = null,
    @SerialName("order_uuid") val orderUuid: String? = null,
    val quantity: Double = 0.0,
    val note: String = "",
    @SerialName("created_on") val createdOn: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

private fun StockProduct.toRemote(organismUuid: String) = RemoteStockProduct(
    uuid = uuid, organismUuid = organismUuid, name = name, unit = unit,
    centralQuantity = centralQuantity, unitPrice = unitPrice, updatedAt = updatedAt
)

private fun StockLivreur.toRemote(organismUuid: String) = RemoteStockLivreur(
    uuid = uuid, organismUuid = organismUuid, name = name, phone = phone,
    userUuid = userUuid.ifBlank { null }, updatedAt = updatedAt
)

private fun MiniStockLine.toRemote(organismUuid: String, livreurUuid: String, productUuid: String) = RemoteMiniStock(
    uuid = uuid, organismUuid = organismUuid, livreurUuid = livreurUuid, productUuid = productUuid,
    quantity = quantity, updatedAt = updatedAt
)

private fun StockOrder.toRemote(organismUuid: String, livreurUuid: String?) = RemoteStockOrder(
    uuid = uuid, organismUuid = organismUuid, livreurUuid = livreurUuid, customerName = customerName,
    status = status, note = note, createdOn = createdOn, updatedAt = updatedAt
)

private fun StockOrderLine.toRemote(orderUuid: String, productUuid: String) = RemoteStockOrderLine(
    uuid = uuid, orderUuid = orderUuid, productUuid = productUuid,
    quantityOrdered = quantityOrdered, quantityDelivered = quantityDelivered, updatedAt = updatedAt
)

private fun StockMovement.toRemote(
    organismUuid: String,
    productUuid: String?,
    livreurUuid: String?,
    orderUuid: String?
) = RemoteStockMovement(
    uuid = uuid, organismUuid = organismUuid, type = type, productUuid = productUuid,
    productName = productName, livreurUuid = livreurUuid, livreurName = livreurName,
    orderUuid = orderUuid, quantity = quantity, note = note, createdOn = createdOn, updatedAt = updatedAt
)

private fun RemoteStockProduct.toLocal(organismId: Long) = StockProduct(
    name = name, unit = unit, centralQuantity = centralQuantity, createdOn = updatedAt,
    organismId = organismId, uuid = uuid, unitPrice = unitPrice, updatedAt = updatedAt
)

private fun RemoteStockLivreur.toLocal(organismId: Long) = StockLivreur(
    name = name, phone = phone, createdOn = updatedAt, organismId = organismId,
    uuid = uuid, userUuid = userUuid.orEmpty(), updatedAt = updatedAt
)

private fun RemoteMiniStock.toLocal(organismId: Long, livreurId: Long, productId: Long) = MiniStockLine(
    livreurId = livreurId, productId = productId, quantity = quantity,
    organismId = organismId, uuid = uuid, updatedAt = updatedAt
)

private fun RemoteStockOrder.toLocal(organismId: Long, livreurId: Long?) = StockOrder(
    organismId = organismId, livreurId = livreurId, customerName = customerName, status = status,
    note = note, createdOn = createdOn, uuid = uuid, updatedAt = updatedAt
)

private fun RemoteStockOrderLine.toLocal(orderId: Long, productId: Long) = StockOrderLine(
    orderId = orderId, productId = productId, quantityOrdered = quantityOrdered,
    quantityDelivered = quantityDelivered, uuid = uuid, updatedAt = updatedAt
)

private fun RemoteStockMovement.toLocal(organismId: Long, productId: Long, livreurId: Long?, orderId: Long?) =
    StockMovement(
        type = type, productId = productId, productName = productName, livreurId = livreurId,
        livreurName = livreurName, quantity = quantity, note = note, createdOn = createdOn,
        organismId = organismId, orderId = orderId, uuid = uuid, updatedAt = updatedAt
    )
