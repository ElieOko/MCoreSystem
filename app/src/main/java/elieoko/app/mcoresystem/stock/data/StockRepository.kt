package elieoko.app.mcoresystem.stock.data

import androidx.room.withTransaction
import elieoko.app.mcoresystem.stock.StockRules
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class StockResult {
    data object Ok : StockResult()
    data class Error(val message: String) : StockResult()
}

class StockRepository(private val database: StockDatabase) {
    private val dao = database.stockDao()

    fun observeProducts(): Flow<List<StockProduct>> = dao.observeProducts()
    fun observeLivreurs(): Flow<List<StockLivreur>> = dao.observeLivreurs()
    fun observeMiniStocks(): Flow<List<MiniStockLine>> = dao.observeMiniStocks()
    fun observeMovements(): Flow<List<StockMovement>> = dao.observeMovements()

    suspend fun createProduct(name: String, unit: String, initialQuantity: Double): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom de l'article est requis")
        val cleanUnit = StockRules.cleanName(unit) ?: "pièce"
        if (initialQuantity < 0.0) {
            return StockResult.Error("La quantité initiale ne peut pas être négative")
        }
        return database.withTransaction {
            val id = dao.insertProduct(
                StockProduct(
                    name = cleanName,
                    unit = cleanUnit,
                    centralQuantity = initialQuantity,
                    createdOn = now()
                )
            )
            if (initialQuantity > 0.0) {
                dao.insertMovement(
                    StockMovement(
                        type = MovementType.ENTREE.name,
                        productId = id,
                        productName = cleanName,
                        quantity = initialQuantity,
                        note = "Stock initial",
                        createdOn = now()
                    )
                )
            }
            StockResult.Ok
        }
    }

    suspend fun renameProduct(productId: Long, name: String, unit: String): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom de l'article est requis")
        val cleanUnit = StockRules.cleanName(unit) ?: "pièce"
        val product = dao.getProduct(productId) ?: return StockResult.Error("Article introuvable")
        dao.updateProduct(product.copy(name = cleanName, unit = cleanUnit))
        return StockResult.Ok
    }

    suspend fun deleteProduct(productId: Long): StockResult {
        if (dao.miniTotalForProduct(productId) > StockRules.EPSILON) {
            return StockResult.Error("Cet article est encore dans le mini-stock d'un livreur")
        }
        val product = dao.getProduct(productId) ?: return StockResult.Error("Article introuvable")
        if (product.centralQuantity > StockRules.EPSILON) {
            return StockResult.Error("Le dépôt contient encore ${StockRules.formatQty(product.centralQuantity)} ${product.unit}")
        }
        dao.deleteProduct(productId)
        return StockResult.Ok
    }

    suspend fun receiveStock(productId: Long, quantity: Double, note: String): StockResult {
        if (quantity <= 0.0) return StockResult.Error("La quantité doit être supérieure à zéro")
        return database.withTransaction {
            val product = dao.getProduct(productId) ?: return@withTransaction StockResult.Error("Article introuvable")
            dao.updateProduct(product.copy(centralQuantity = StockRules.add(product.centralQuantity, quantity)))
            recordCentral(product, quantity, note, MovementType.ENTREE)
            StockResult.Ok
        }
    }

    suspend fun withdrawStock(productId: Long, quantity: Double, note: String): StockResult {
        return database.withTransaction {
            val product = dao.getProduct(productId) ?: return@withTransaction StockResult.Error("Article introuvable")
            val error = StockRules.giveError(product.centralQuantity, quantity)
            if (error != null) return@withTransaction StockResult.Error(error)
            dao.updateProduct(product.copy(centralQuantity = StockRules.subtract(product.centralQuantity, quantity)))
            recordCentral(product, quantity, note, MovementType.RETRAIT)
            StockResult.Ok
        }
    }

    suspend fun createLivreur(name: String, phone: String): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom du livreur est requis")
        dao.insertLivreur(
            StockLivreur(
                name = cleanName,
                phone = phone.trim(),
                createdOn = now()
            )
        )
        return StockResult.Ok
    }

    suspend fun renameLivreur(livreurId: Long, name: String, phone: String): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom du livreur est requis")
        val livreur = dao.getLivreur(livreurId) ?: return StockResult.Error("Livreur introuvable")
        dao.updateLivreur(livreur.copy(name = cleanName, phone = phone.trim()))
        return StockResult.Ok
    }

    suspend fun deleteLivreur(livreurId: Long): StockResult {
        if (dao.miniTotalForLivreur(livreurId) > StockRules.EPSILON) {
            return StockResult.Error("Ce livreur a encore un mini-stock. Faites un retour ou une sortie d'abord")
        }
        dao.deleteMiniForLivreur(livreurId)
        dao.deleteLivreur(livreurId)
        return StockResult.Ok
    }

    suspend fun giveMiniStock(livreurId: Long, productId: Long, quantity: Double, note: String): StockResult {
        return database.withTransaction {
            val product = dao.getProduct(productId) ?: return@withTransaction StockResult.Error("Article introuvable")
            val livreur = dao.getLivreur(livreurId) ?: return@withTransaction StockResult.Error("Livreur introuvable")
            val error = StockRules.giveError(product.centralQuantity, quantity)
            if (error != null) return@withTransaction StockResult.Error(error)
            dao.updateProduct(
                product.copy(centralQuantity = StockRules.subtract(product.centralQuantity, quantity))
            )
            val existing = dao.getMini(livreurId, productId)
            if (existing == null) {
                dao.insertMini(MiniStockLine(livreurId = livreurId, productId = productId, quantity = quantity))
            } else {
                dao.updateMini(existing.copy(quantity = StockRules.add(existing.quantity, quantity)))
            }
            dao.insertMovement(
                StockMovement(
                    type = MovementType.DOTATION.name,
                    productId = product.id,
                    productName = product.name,
                    livreurId = livreur.id,
                    livreurName = livreur.name,
                    quantity = quantity,
                    note = note.trim(),
                    createdOn = now()
                )
            )
            StockResult.Ok
        }
    }

    suspend fun returnMiniStock(livreurId: Long, productId: Long, quantity: Double, note: String): StockResult {
        return moveOutOfMini(livreurId, productId, quantity, note, MovementType.RETOUR, backToCentral = true)
    }

    suspend fun recordSortie(livreurId: Long, productId: Long, quantity: Double, note: String): StockResult {
        return moveOutOfMini(livreurId, productId, quantity, note, MovementType.SORTIE, backToCentral = false)
    }

    private suspend fun moveOutOfMini(
        livreurId: Long,
        productId: Long,
        quantity: Double,
        note: String,
        type: MovementType,
        backToCentral: Boolean
    ): StockResult = database.withTransaction {
        val product = dao.getProduct(productId) ?: return@withTransaction StockResult.Error("Article introuvable")
        val livreur = dao.getLivreur(livreurId) ?: return@withTransaction StockResult.Error("Livreur introuvable")
        val line = dao.getMini(livreurId, productId)
        val available = line?.quantity ?: 0.0
        val error = StockRules.takeFromMiniError(available, quantity)
        if (error != null || line == null) {
            return@withTransaction StockResult.Error(error ?: "Mini-stock insuffisant (0 disponible)")
        }
        val left = StockRules.subtract(line.quantity, quantity)
        if (StockRules.isGone(left)) dao.deleteMini(line.id) else dao.updateMini(line.copy(quantity = left))
        if (backToCentral) {
            dao.updateProduct(product.copy(centralQuantity = StockRules.add(product.centralQuantity, quantity)))
        }
        dao.insertMovement(
            StockMovement(
                type = type.name,
                productId = product.id,
                productName = product.name,
                livreurId = livreur.id,
                livreurName = livreur.name,
                quantity = quantity,
                note = note.trim(),
                createdOn = now()
            )
        )
        StockResult.Ok
    }

    private suspend fun recordCentral(
        product: StockProduct,
        quantity: Double,
        note: String,
        type: MovementType
    ) {
        dao.insertMovement(
            StockMovement(
                type = type.name,
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                note = note.trim(),
                createdOn = now()
            )
        )
    }

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.FRANCE).format(Date())
}
