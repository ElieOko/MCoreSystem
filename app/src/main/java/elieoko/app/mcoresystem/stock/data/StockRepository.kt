package elieoko.app.mcoresystem.stock.data

import androidx.room.withTransaction
import elieoko.app.mcoresystem.domain.util.SecurityUtil
import elieoko.app.mcoresystem.stock.StockRules
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class StockResult {
    data object Ok : StockResult()
    data class Error(val message: String) : StockResult()
}

sealed class StockAuth {
    data class Success(val session: StockSession) : StockAuth()
    data class Error(val message: String) : StockAuth()
}

private class StockFailure(message: String) : Exception(message)

class StockRepository(private val database: StockDatabase) {
    private val dao = database.stockDao()

    fun observeProducts(): Flow<List<StockProduct>> = dao.observeProducts()
    fun observeLivreurs(): Flow<List<StockLivreur>> = dao.observeLivreurs()
    fun observeMiniStocks(): Flow<List<MiniStockLine>> = dao.observeMiniStocks()
    fun observeMovements(): Flow<List<StockMovement>> = dao.observeMovements()
    fun observeAccountCount(): Flow<Int> = dao.observeAccountCount()
    fun observeAccounts(organismId: Long): Flow<List<StockAccount>> = dao.observeAccounts(organismId)
    fun observeOrders(): Flow<List<StockOrder>> = dao.observeOrders()
    fun observeOrderLines(): Flow<List<StockOrderLine>> = dao.observeOrderLines()

    suspend fun registerOrganization(name: String, username: String, password: String): StockAuth {
        val orgName = StockRules.cleanName(name) ?: return StockAuth.Error("Le nom de l'organisation est requis")
        val login = StockRules.normalizeUsername(username)
            ?: return StockAuth.Error("Identifiant invalide : 3 caractères minimum, sans espace")
        StockRules.passwordError(password)?.let { return StockAuth.Error(it) }
        if (dao.countOrganisms() > 0 && dao.allAccounts().isNotEmpty()) {
            return StockAuth.Error("Une organisation existe déjà sur cet appareil")
        }
        return database.withTransaction {
            val createdOn = now()
            val organismUuid = UUID.randomUUID().toString()
            val organismId = dao.insertOrganism(
                StockOrganism(uuid = organismUuid, name = orgName, createdOn = createdOn)
            )
            dao.claimProducts(organismId)
            dao.claimLivreurs(organismId)
            dao.claimMiniStocks(organismId)
            dao.claimMovements(organismId)
            val accountId = dao.insertAccount(
                StockAccount(
                    organismId = organismId,
                    username = login,
                    passwordHash = SecurityUtil.sha256(password),
                    role = StockRole.OWNER.name,
                    createdOn = createdOn
                )
            )
            StockAuth.Success(
                StockSession(
                    accountId = accountId,
                    organismId = organismId,
                    organismUuid = organismUuid,
                    organismName = orgName,
                    username = login,
                    role = StockRole.OWNER.name
                )
            )
        }
    }

    suspend fun login(username: String, password: String): StockAuth {
        val login = username.trim()
        if (login.isEmpty() || password.isEmpty()) {
            return StockAuth.Error("Identifiant et mot de passe requis")
        }
        val hash = SecurityUtil.sha256(password)
        val matches = dao.allAccounts().filter {
            it.username.equals(login, ignoreCase = true) && it.passwordHash == hash
        }
        val account = when (matches.size) {
            0 -> return StockAuth.Error("Identifiants incorrects")
            1 -> matches.first()
            else -> return StockAuth.Error("Plusieurs comptes correspondent. Précisez l'identifiant")
        }
        val organism = dao.getOrganism(account.organismId)
            ?: return StockAuth.Error("Organisation introuvable")
        val livreur = account.livreurId?.let { dao.getLivreur(it) }
        return StockAuth.Success(
            StockSession(
                accountId = account.id,
                organismId = organism.id,
                organismUuid = organism.uuid,
                organismName = organism.name,
                username = account.username,
                role = account.role,
                livreurId = livreur?.id ?: 0,
                livreurName = livreur?.name ?: ""
            )
        )
    }

    suspend fun createProduct(organismId: Long, name: String, unit: String, initialQuantity: Double): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom de l'article est requis")
        val cleanUnit = StockRules.cleanName(unit) ?: "pièce"
        if (initialQuantity < 0.0) {
            return StockResult.Error("La quantité initiale ne peut pas être négative")
        }
        return database.withTransaction {
            val createdOn = now()
            val id = dao.insertProduct(
                StockProduct(
                    name = cleanName,
                    unit = cleanUnit,
                    centralQuantity = initialQuantity,
                    createdOn = createdOn,
                    organismId = organismId,
                    uuid = UUID.randomUUID().toString()
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
                        createdOn = createdOn,
                        organismId = organismId
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

    suspend fun createLivreur(
        organismId: Long,
        name: String,
        phone: String,
        username: String,
        password: String
    ): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom du livreur est requis")
        val login = StockRules.normalizeUsername(username)
            ?: return StockResult.Error("Identifiant invalide : 3 caractères minimum, sans espace")
        StockRules.passwordError(password)?.let { return StockResult.Error(it) }
        if (dao.usernameTaken(organismId, login) > 0) {
            return StockResult.Error("Cet identifiant est déjà utilisé dans l'organisation")
        }
        return database.withTransaction {
            val createdOn = now()
            val livreurId = dao.insertLivreur(
                StockLivreur(
                    name = cleanName,
                    phone = phone.trim(),
                    createdOn = createdOn,
                    organismId = organismId,
                    uuid = UUID.randomUUID().toString()
                )
            )
            dao.insertAccount(
                StockAccount(
                    organismId = organismId,
                    livreurId = livreurId,
                    username = login,
                    passwordHash = SecurityUtil.sha256(password),
                    role = StockRole.LIVREUR.name,
                    createdOn = createdOn
                )
            )
            StockResult.Ok
        }
    }

    suspend fun renameLivreur(
        livreurId: Long,
        name: String,
        phone: String,
        username: String,
        password: String?
    ): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom du livreur est requis")
        val login = StockRules.normalizeUsername(username)
            ?: return StockResult.Error("Identifiant invalide : 3 caractères minimum, sans espace")
        if (!password.isNullOrEmpty()) {
            StockRules.passwordError(password)?.let { return StockResult.Error(it) }
        }
        val livreur = dao.getLivreur(livreurId) ?: return StockResult.Error("Livreur introuvable")
        val account = dao.accountForLivreur(livreurId)
            ?: return StockResult.Error("Compte du livreur introuvable")
        if (!account.username.equals(login, ignoreCase = true) &&
            dao.usernameTaken(livreur.organismId, login) > 0
        ) {
            return StockResult.Error("Cet identifiant est déjà utilisé dans l'organisation")
        }
        dao.updateLivreur(livreur.copy(name = cleanName, phone = phone.trim()))
        dao.updateAccount(
            account.copy(
                username = login,
                passwordHash = if (password.isNullOrEmpty()) account.passwordHash else SecurityUtil.sha256(password)
            )
        )
        return StockResult.Ok
    }

    suspend fun deleteLivreur(livreurId: Long): StockResult {
        if (dao.miniTotalForLivreur(livreurId) > StockRules.EPSILON) {
            return StockResult.Error("Ce livreur a encore un mini-stock. Faites un retour ou une sortie d'abord")
        }
        dao.deleteAccountForLivreur(livreurId)
        dao.deleteMiniForLivreur(livreurId)
        dao.deleteLivreur(livreurId)
        return StockResult.Ok
    }

    suspend fun giveMiniStock(livreurId: Long, productId: Long, quantity: Double, note: String): StockResult {
        return database.withTransaction {
            val product = dao.getProduct(productId) ?: return@withTransaction StockResult.Error("Article introuvable")
            val livreur = dao.getLivreur(livreurId) ?: return@withTransaction StockResult.Error("Livreur introuvable")
            if (product.organismId != livreur.organismId) {
                return@withTransaction StockResult.Error("Cet article n'appartient pas à la même organisation")
            }
            val error = StockRules.giveError(product.centralQuantity, quantity)
            if (error != null) return@withTransaction StockResult.Error(error)
            dao.updateProduct(
                product.copy(centralQuantity = StockRules.subtract(product.centralQuantity, quantity))
            )
            val existing = dao.getMini(livreurId, productId)
            if (existing == null) {
                dao.insertMini(
                    MiniStockLine(
                        livreurId = livreurId,
                        productId = productId,
                        quantity = quantity,
                        organismId = product.organismId
                    )
                )
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
                    createdOn = now(),
                    organismId = product.organismId
                )
            )
            StockResult.Ok
        }
    }

    suspend fun returnMiniStock(livreurId: Long, productId: Long, quantity: Double, note: String): StockResult {
        return moveOutOfMini(livreurId, productId, quantity, note, MovementType.RETOUR, backToCentral = true)
    }

    suspend fun recordSortie(
        livreurId: Long,
        productId: Long,
        quantity: Double,
        note: String,
        orderId: Long? = null
    ): StockResult {
        return moveOutOfMini(
            livreurId, productId, quantity, note, MovementType.SORTIE,
            backToCentral = false, orderId = orderId
        )
    }

    suspend fun deliver(
        organismId: Long,
        livreurId: Long,
        lines: List<DeliveryLine>,
        orderId: Long?,
        note: String
    ): StockResult = try {
        database.withTransaction { deliverInTransaction(organismId, livreurId, lines, orderId, note) }
    } catch (failure: StockFailure) {
        StockResult.Error(failure.message ?: "Livraison impossible")
    }

    private suspend fun deliverInTransaction(
        organismId: Long,
        livreurId: Long,
        lines: List<DeliveryLine>,
        orderId: Long?,
        note: String
    ): StockResult {
        val livreur = dao.getLivreur(livreurId) ?: throw StockFailure("Livreur introuvable")
        if (livreur.organismId != organismId) {
            throw StockFailure("Ce livreur n'appartient pas à votre organisation")
        }
        val outgoing = lines.filter { it.quantity > 0.0 }
        if (outgoing.isEmpty()) throw StockFailure("Indiquez au moins une quantité")
        for (line in outgoing) {
            val available = dao.getMini(livreurId, line.productId)?.quantity ?: 0.0
            val error = StockRules.takeFromMiniError(available, line.quantity)
            if (error != null) {
                val product = dao.getProduct(line.productId)
                throw StockFailure("${product?.name ?: "Article"} : $error")
            }
        }
        val order = if (orderId == null) null else {
            val found = dao.getOrder(orderId) ?: throw StockFailure("Commande introuvable")
            if (found.organismId != organismId || found.livreurId != livreurId) {
                throw StockFailure("Cette commande n'est pas assignée à ce livreur")
            }
            if (found.status == OrderStatus.ANNULEE.name || found.status == OrderStatus.LIVREE.name) {
                throw StockFailure("Cette commande n'est plus ouverte")
            }
            found
        }
        val deliveryNote = deliveryNote(order, note)
        for (line in outgoing) {
            val result = applyMiniExit(
                livreurId, line.productId, line.quantity, deliveryNote,
                MovementType.SORTIE, backToCentral = false, orderId = order?.id
            )
            if (result is StockResult.Error) throw StockFailure(result.message)
            if (order != null) {
                val orderLine = dao.getOrderLine(order.id, line.productId)
                if (orderLine != null) {
                    dao.updateOrderLine(
                        orderLine.copy(quantityDelivered = StockRules.add(orderLine.quantityDelivered, line.quantity))
                    )
                }
            }
        }
        if (order != null) {
            val refreshed = dao.linesForOrder(order.id)
            val status = StockRules.nextOrderStatus(refreshed.map { it.quantityOrdered to it.quantityDelivered })
            dao.updateOrder(order.copy(status = status))
        }
        return StockResult.Ok
    }

    suspend fun createOrder(
        organismId: Long,
        customerName: String,
        livreurId: Long?,
        note: String,
        lines: List<DeliveryLine>
    ): StockResult {
        val customer = StockRules.cleanName(customerName)
            ?: return StockResult.Error("Le nom du client est requis")
        val merged = lines
            .filter { it.quantity > 0.0 }
            .groupBy { it.productId }
            .map { (productId, group) -> DeliveryLine(productId, group.sumOf { it.quantity }) }
        if (merged.isEmpty()) return StockResult.Error("Ajoutez au moins un article à la commande")
        val livreur = if (livreurId == null) null else {
            val found = dao.getLivreur(livreurId) ?: return StockResult.Error("Livreur introuvable")
            if (found.organismId != organismId) {
                return StockResult.Error("Ce livreur n'appartient pas à votre organisation")
            }
            found
        }
        return database.withTransaction {
            for (line in merged) {
                val product = dao.getProduct(line.productId) ?: return@withTransaction StockResult.Error("Article introuvable")
                if (product.organismId != organismId) {
                    return@withTransaction StockResult.Error("${product.name} n'appartient pas à l'organisation")
                }
            }
            val orderId = dao.insertOrder(
                StockOrder(
                    organismId = organismId,
                    livreurId = livreur?.id,
                    customerName = customer,
                    status = if (livreur == null) OrderStatus.BROUILLON.name else OrderStatus.ASSIGNEE.name,
                    note = note.trim(),
                    createdOn = now()
                )
            )
            merged.forEach { line ->
                dao.insertOrderLine(
                    StockOrderLine(
                        orderId = orderId,
                        productId = line.productId,
                        quantityOrdered = line.quantity
                    )
                )
            }
            StockResult.Ok
        }
    }

    suspend fun cancelOrder(orderId: Long, organismId: Long): StockResult {
        val order = dao.getOrder(orderId) ?: return StockResult.Error("Commande introuvable")
        if (order.organismId != organismId) return StockResult.Error("Commande introuvable")
        if (order.status == OrderStatus.LIVREE.name) {
            return StockResult.Error("Une commande déjà livrée ne peut pas être annulée")
        }
        dao.updateOrder(order.copy(status = OrderStatus.ANNULEE.name))
        return StockResult.Ok
    }

    private suspend fun moveOutOfMini(
        livreurId: Long,
        productId: Long,
        quantity: Double,
        note: String,
        type: MovementType,
        backToCentral: Boolean,
        orderId: Long? = null
    ): StockResult = database.withTransaction {
        applyMiniExit(livreurId, productId, quantity, note, type, backToCentral, orderId)
    }

    private suspend fun applyMiniExit(
        livreurId: Long,
        productId: Long,
        quantity: Double,
        note: String,
        type: MovementType,
        backToCentral: Boolean,
        orderId: Long? = null
    ): StockResult {
        val product = dao.getProduct(productId) ?: return StockResult.Error("Article introuvable")
        val livreur = dao.getLivreur(livreurId) ?: return StockResult.Error("Livreur introuvable")
        val line = dao.getMini(livreurId, productId)
        val available = line?.quantity ?: 0.0
        val error = StockRules.takeFromMiniError(available, quantity)
        if (error != null || line == null) {
            return StockResult.Error(error ?: "Mini-stock insuffisant (0 disponible)")
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
                createdOn = now(),
                organismId = product.organismId,
                orderId = orderId
            )
        )
        return StockResult.Ok
    }

    private fun deliveryNote(order: StockOrder?, note: String): String {
        val extra = note.trim()
        val prefix = order?.let { "Commande ${it.customerName}" }.orEmpty()
        return listOf(prefix, extra).filter { it.isNotBlank() }.joinToString(" — ")
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
                createdOn = now(),
                organismId = product.organismId
            )
        )
    }

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.FRANCE).format(Date())
}
