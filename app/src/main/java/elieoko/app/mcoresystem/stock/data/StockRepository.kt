package elieoko.app.mcoresystem.stock.data

import androidx.room.withTransaction
import elieoko.app.mcoresystem.domain.util.SecurityUtil
import elieoko.app.mcoresystem.domain.util.TimeUtil
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

data class LivreurProfile(
    val userUuid: String,
    val name: String,
    val phone: String
)

sealed class StockAuth {
    data class Success(val session: StockSession) : StockAuth()
    data class Error(val message: String) : StockAuth()
}

private class StockFailure(message: String) : Exception(message)

class StockRepository(private val database: StockDatabase) {
    private val dao = database.stockDao()

    fun observeProducts(organismId: Long): Flow<List<StockProduct>> = dao.observeProducts(organismId)
    fun observeLivreurs(organismId: Long): Flow<List<StockLivreur>> = dao.observeLivreurs(organismId)
    fun observeMiniStocks(organismId: Long): Flow<List<MiniStockLine>> = dao.observeMiniStocks(organismId)
    fun observeMovements(organismId: Long): Flow<List<StockMovement>> = dao.observeMovements(organismId)
    fun observeAccountCount(): Flow<Int> = dao.observeAccountCount()
    fun observeAccounts(organismId: Long): Flow<List<StockAccount>> = dao.observeAccounts(organismId)
    fun observeOrders(organismId: Long): Flow<List<StockOrder>> = dao.observeOrders(organismId)
    fun observeOrderLines(organismId: Long): Flow<List<StockOrderLine>> = dao.observeOrderLines(organismId)

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
                StockOrganism(uuid = organismUuid, name = orgName, createdOn = createdOn, updatedAt = iso())
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

    suspend fun createProduct(
        organismId: Long,
        name: String,
        unit: String,
        initialQuantity: Double,
        unitPrice: Double
    ): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom de l'article est requis")
        val cleanUnit = StockRules.cleanName(unit) ?: "pièce"
        if (initialQuantity < 0.0) {
            return StockResult.Error("La quantité initiale ne peut pas être négative")
        }
        if (unitPrice < 0.0) {
            return StockResult.Error("Le montant ne peut pas être négatif")
        }
        return database.withTransaction {
            val createdOn = now()
            val stamp = iso()
            val id = dao.insertProduct(
                StockProduct(
                    name = cleanName,
                    unit = cleanUnit,
                    centralQuantity = initialQuantity,
                    createdOn = createdOn,
                    organismId = organismId,
                    uuid = UUID.randomUUID().toString(),
                    unitPrice = unitPrice,
                    updatedAt = stamp
                )
            )
            if (initialQuantity > 0.0) {
                dao.insertMovement(
                    movement(
                        type = MovementType.ENTREE,
                        productId = id,
                        productName = cleanName,
                        quantity = initialQuantity,
                        note = "Stock initial",
                        organismId = organismId
                    )
                )
            }
            StockResult.Ok
        }
    }

    suspend fun renameProduct(productId: Long, name: String, unit: String, unitPrice: Double): StockResult {
        val cleanName = StockRules.cleanName(name)
            ?: return StockResult.Error("Le nom de l'article est requis")
        val cleanUnit = StockRules.cleanName(unit) ?: "pièce"
        if (unitPrice < 0.0) return StockResult.Error("Le montant ne peut pas être négatif")
        val product = dao.getProduct(productId) ?: return StockResult.Error("Article introuvable")
        dao.updateProduct(
            product.copy(name = cleanName, unit = cleanUnit, unitPrice = unitPrice, updatedAt = iso())
        )
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
            dao.updateProduct(
                product.copy(
                    centralQuantity = StockRules.add(product.centralQuantity, quantity),
                    updatedAt = iso()
                )
            )
            recordCentral(product, quantity, note, MovementType.ENTREE)
            StockResult.Ok
        }
    }

    suspend fun withdrawStock(productId: Long, quantity: Double, note: String): StockResult {
        return database.withTransaction {
            val product = dao.getProduct(productId) ?: return@withTransaction StockResult.Error("Article introuvable")
            val error = StockRules.giveError(product.centralQuantity, quantity)
            if (error != null) return@withTransaction StockResult.Error(error)
            dao.updateProduct(
                product.copy(
                    centralQuantity = StockRules.subtract(product.centralQuantity, quantity),
                    updatedAt = iso()
                )
            )
            recordCentral(product, quantity, note, MovementType.RETRAIT)
            StockResult.Ok
        }
    }

    suspend fun createLivreur(
        organismId: Long,
        name: String,
        phone: String,
        username: String,
        password: String,
        userUuid: String
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
                    uuid = UUID.randomUUID().toString(),
                    userUuid = userUuid,
                    updatedAt = iso()
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
        dao.updateLivreur(livreur.copy(name = cleanName, phone = phone.trim(), updatedAt = iso()))
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
                product.copy(
                    centralQuantity = StockRules.subtract(product.centralQuantity, quantity),
                    updatedAt = iso()
                )
            )
            val existing = dao.getMini(livreurId, productId)
            if (existing == null) {
                dao.insertMini(
                    MiniStockLine(
                        livreurId = livreurId,
                        productId = productId,
                        quantity = quantity,
                        organismId = product.organismId,
                        uuid = UUID.randomUUID().toString(),
                        updatedAt = iso()
                    )
                )
            } else {
                dao.updateMini(
                    existing.copy(quantity = StockRules.add(existing.quantity, quantity), updatedAt = iso())
                )
            }
            dao.insertMovement(
                movement(
                    type = MovementType.DOTATION,
                    productId = product.id,
                    productName = product.name,
                    livreurId = livreur.id,
                    livreurName = livreur.name,
                    quantity = quantity,
                    note = note,
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
                        orderLine.copy(
                            quantityDelivered = StockRules.add(orderLine.quantityDelivered, line.quantity),
                            updatedAt = iso()
                        )
                    )
                }
            }
        }
        if (order != null) {
            val refreshed = dao.linesForOrder(order.id)
            val status = StockRules.nextOrderStatus(refreshed.map { it.quantityOrdered to it.quantityDelivered })
            dao.updateOrder(order.copy(status = status, updatedAt = iso()))
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
            val stamp = iso()
            val orderId = dao.insertOrder(
                StockOrder(
                    organismId = organismId,
                    livreurId = livreur?.id,
                    customerName = customer,
                    status = if (livreur == null) OrderStatus.BROUILLON.name else OrderStatus.ASSIGNEE.name,
                    note = note.trim(),
                    createdOn = now(),
                    uuid = UUID.randomUUID().toString(),
                    updatedAt = stamp
                )
            )
            merged.forEach { line ->
                dao.insertOrderLine(
                    StockOrderLine(
                        orderId = orderId,
                        productId = line.productId,
                        quantityOrdered = line.quantity,
                        uuid = UUID.randomUUID().toString(),
                        updatedAt = stamp
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
        dao.updateOrder(order.copy(status = OrderStatus.ANNULEE.name, updatedAt = iso()))
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
        if (StockRules.isGone(left)) {
            dao.deleteMini(line.id)
        } else {
            dao.updateMini(line.copy(quantity = left, updatedAt = iso()))
        }
        if (backToCentral) {
            dao.updateProduct(
                product.copy(
                    centralQuantity = StockRules.add(product.centralQuantity, quantity),
                    updatedAt = iso()
                )
            )
        }
        dao.insertMovement(
            movement(
                type = type,
                productId = product.id,
                productName = product.name,
                livreurId = livreur.id,
                livreurName = livreur.name,
                quantity = quantity,
                note = note,
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

    suspend fun livreurByUserUuid(userUuid: String, organismId: Long): StockLivreur? {
        if (userUuid.isBlank()) return null
        return dao.livreurByUserUuid(userUuid, organismId)
    }

    /**
     * Crée le profil stock des comptes déjà présents dans la table utilisateurs
     * de cette organisation. Ne touche pas aux lignes d'une autre organisation.
     */
    suspend fun ensureLivreurProfiles(organismId: Long, profiles: List<LivreurProfile>) {
        val stamp = iso()
        for (profile in profiles) {
            if (profile.userUuid.isBlank()) continue
            if (dao.livreurByUserUuid(profile.userUuid, organismId) != null) continue
            dao.insertLivreur(
                StockLivreur(
                    name = profile.name.ifBlank { profile.userUuid },
                    phone = profile.phone,
                    createdOn = now(),
                    organismId = organismId,
                    uuid = UUID.randomUUID().toString(),
                    userUuid = profile.userUuid,
                    updatedAt = stamp
                )
            )
        }
    }

    /**
     * Une ligne locale par uuid d'organisation. Le cache d'une organisation
     * n'est jamais réécrit avec l'uuid d'une autre.
     */
    suspend fun ensureOrganism(uuid: String, name: String): StockOrganism {
        val cleanName = name.trim().ifBlank { "Organisation" }
        val existing = dao.organismByUuid(uuid)
        if (existing != null) {
            if (existing.name == cleanName) return existing
            val updated = existing.copy(name = cleanName, updatedAt = iso())
            dao.updateOrganism(updated)
            return updated
        }
        val createdOn = now()
        val adoptOrphans = dao.countOrganisms() == 0
        val id = dao.insertOrganism(
            StockOrganism(uuid = uuid, name = cleanName, createdOn = createdOn, updatedAt = iso())
        )
        if (adoptOrphans) {
            dao.claimProducts(id)
            dao.claimLivreurs(id)
            dao.claimMiniStocks(id)
            dao.claimMovements(id)
            dao.claimOrders(id)
        }
        return dao.getOrganism(id)
            ?: StockOrganism(id = id, uuid = uuid, name = cleanName, createdOn = createdOn, updatedAt = iso())
    }

    private suspend fun recordCentral(
        product: StockProduct,
        quantity: Double,
        note: String,
        type: MovementType
    ) {
        dao.insertMovement(
            movement(
                type = type,
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                note = note,
                organismId = product.organismId
            )
        )
    }

    private fun movement(
        type: MovementType,
        productId: Long,
        productName: String,
        quantity: Double,
        note: String,
        organismId: Long,
        livreurId: Long? = null,
        livreurName: String? = null,
        orderId: Long? = null
    ) = StockMovement(
        type = type.name,
        productId = productId,
        productName = productName,
        livreurId = livreurId,
        livreurName = livreurName,
        quantity = quantity,
        note = note.trim(),
        createdOn = now(),
        organismId = organismId,
        orderId = orderId,
        uuid = UUID.randomUUID().toString(),
        updatedAt = iso()
    )

    private fun now(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.FRANCE).format(Date())

    private fun iso(): String = TimeUtil.nowIso()
}
