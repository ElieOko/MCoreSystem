package elieoko.app.mcoresystem.stock

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.MCoreApplication
import elieoko.app.mcoresystem.data.remote.ChangeTracker
import elieoko.app.mcoresystem.domain.model.room.SyncQueueModel
import elieoko.app.mcoresystem.domain.model.room.UserModel
import elieoko.app.mcoresystem.stock.data.DeliveryLine
import elieoko.app.mcoresystem.stock.data.MiniStockLine
import elieoko.app.mcoresystem.stock.data.StockAuth
import elieoko.app.mcoresystem.stock.data.StockDatabase
import elieoko.app.mcoresystem.stock.data.StockLivreur
import elieoko.app.mcoresystem.stock.data.StockOrder
import elieoko.app.mcoresystem.stock.data.StockProduct
import elieoko.app.mcoresystem.stock.data.StockRepository
import elieoko.app.mcoresystem.stock.data.StockResult
import elieoko.app.mcoresystem.stock.data.StockSession
import elieoko.app.mcoresystem.stock.data.StockSessionStore
import elieoko.app.mcoresystem.stock.data.StockSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class StockAccess(
    val organismUuid: String,
    val organismName: String,
    val financeOrganismId: Int,
    val userUuid: String,
    val username: String,
    val role: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StockDatabase.get(application)
    private val repository = StockRepository(database)
    private val sync = StockSync(database)
    private val sessionStore = StockSessionStore(application)
    private var financeOrganismId: Int = 0
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val sessionState = MutableStateFlow<StockSession?>(null)
    private val readyState = MutableStateFlow(false)

    val message = messages.asSharedFlow()
    val session = sessionState.asStateFlow()
    val ready = readyState.asStateFlow()
    val accountCount = repository.observeAccountCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    private val organismId = sessionState

    val products = combine(repository.observeProducts(), organismId) { items, current ->
        items.filter { current != null && it.organismId == current.organismId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val livreurs = combine(repository.observeLivreurs(), organismId) { items, current ->
        items.filter { current != null && it.organismId == current.organismId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val miniStocks = combine(repository.observeMiniStocks(), organismId) { items, current ->
        items.filter { current != null && it.organismId == current.organismId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movements = combine(repository.observeMovements(), organismId) { items, current ->
        items.filter { current != null && it.organismId == current.organismId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orders = combine(repository.observeOrders(), organismId) { items, current ->
        items.filter { current != null && it.organismId == current.organismId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orderLines = combine(repository.observeOrderLines(), orders) { lines, currentOrders ->
        val ids = currentOrders.map { it.id }.toSet()
        lines.filter { it.orderId in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts = sessionState.flatMapLatest { current ->
        if (current == null) flowOf(emptyList()) else repository.observeAccounts(current.organismId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bind(access: StockAccess) {
        val current = sessionState.value
        if (
            readyState.value &&
            current != null &&
            current.organismUuid == access.organismUuid &&
            current.username == access.username &&
            current.role == access.role
        ) {
            return
        }
        viewModelScope.launch {
            financeOrganismId = access.financeOrganismId
            val organism = repository.ensureOrganism(access.organismUuid, access.organismName)
            val livreur = if (access.role.equals("LIVREUR", ignoreCase = true)) {
                repository.livreurByUserUuid(access.userUuid)
            } else {
                null
            }
            val opened = StockSession(
                accountId = 0,
                organismId = organism.id,
                organismUuid = organism.uuid,
                organismName = organism.name,
                username = access.username,
                role = access.role,
                livreurId = livreur?.id ?: 0,
                livreurName = livreur?.name ?: ""
            )
            sessionStore.save(opened)
            sessionState.value = opened
            readyState.value = true
            if (opened.isLivreur && livreur == null) {
                messages.emit("Ce compte n'est pas encore lié à un livreur du stock.")
            }
            publishCloud()?.let { messages.emit("Cloud : $it") }
        }
    }

    fun registerOrganization(name: String, username: String, password: String, confirm: String, onDone: (Boolean) -> Unit = {}) {
        if (password != confirm) {
            messages.tryEmit("Les mots de passe ne correspondent pas")
            onDone(false)
            return
        }
        viewModelScope.launch {
            when (val result = repository.registerOrganization(name, username, password)) {
                is StockAuth.Success -> {
                    sessionStore.save(result.session)
                    messages.emit("Espace de l'organisation créé")
                    onDone(true)
                }
                is StockAuth.Error -> {
                    messages.emit(result.message)
                    onDone(false)
                }
            }
        }
    }

    fun login(username: String, password: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.login(username, password)) {
                is StockAuth.Success -> {
                    sessionStore.save(result.session)
                    onDone(true)
                }
                is StockAuth.Error -> {
                    messages.emit(result.message)
                    onDone(false)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionStore.clear()
            sessionState.value = null
        }
    }

    fun createProduct(
        name: String,
        unit: String,
        initialQuantityRaw: String,
        priceRaw: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val organism = ownerOrganism() ?: run { onDone(false); return }
        val quantity = if (initialQuantityRaw.isBlank()) 0.0 else StockRules.parseQuantity(initialQuantityRaw)
        if (initialQuantityRaw.isNotBlank() && quantity == null) {
            messages.tryEmit("Quantité initiale invalide")
            onDone(false)
            return
        }
        val price = StockRules.parseAmount(priceRaw)
        if (price == null) {
            messages.tryEmit("Montant invalide")
            onDone(false)
            return
        }
        launch("Article enregistré", onDone) {
            repository.createProduct(organism, name, unit, quantity ?: 0.0, price)
        }
    }

    fun renameProduct(
        product: StockProduct,
        name: String,
        unit: String,
        priceRaw: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        val price = StockRules.parseAmount(priceRaw)
        if (price == null) {
            messages.tryEmit("Montant invalide")
            onDone(false)
            return
        }
        launch("Article mis à jour", onDone) { repository.renameProduct(product.id, name, unit, price) }
    }

    fun deleteProduct(product: StockProduct, onDone: (Boolean) -> Unit = {}) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Article supprimé", onDone) { repository.deleteProduct(product.id) }
    }

    fun receiveStock(product: StockProduct, quantityRaw: String, note: String, onDone: (Boolean) -> Unit = {}) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        launch("Entrée enregistrée au dépôt", onDone) { repository.receiveStock(product.id, quantity, note) }
    }

    fun withdrawStock(product: StockProduct, quantityRaw: String, note: String, onDone: (Boolean) -> Unit = {}) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        launch("Retrait enregistré", onDone) { repository.withdrawStock(product.id, quantity, note) }
    }

    fun createLivreur(
        name: String,
        phone: String,
        username: String,
        password: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val organism = ownerOrganism() ?: run { onDone(false); return }
        val login = StockRules.normalizeUsername(username)
        if (login == null) {
            messages.tryEmit("Identifiant invalide : 3 caractères minimum, sans espace")
            onDone(false)
            return
        }
        StockRules.passwordError(password)?.let {
            messages.tryEmit(it)
            onDone(false)
            return
        }
        launch("Livreur enregistré. Il se connecte avec la même page que l'organisation.", onDone) {
            val app = getApplication<MCoreApplication>()
            val users = app.database.userDao()
            if (users.findByUsername(login) != null) {
                return@launch StockResult.Error("Cet identifiant est déjà utilisé")
            }
            val userUuid = UUID.randomUUID().toString()
            var nextId = users.countUsers() + 1
            while (users.loadAllById(nextId).isNotEmpty()) nextId++
            app.userRepository.insert(
                UserModel(
                    id = nextId,
                    username = login,
                    phone = phone.trim().ifBlank { null },
                    email = null,
                    password = password,
                    organismId = financeOrganismId.takeIf { it > 0 } ?: 1,
                    role = "LIVREUR",
                    uuid = userUuid
                )
            )
            val result = repository.createLivreur(organism, name, phone, username, password, userUuid)
            if (result is StockResult.Error) {
                users.findByUuid(userUuid)?.let { created ->
                    users.delete(created)
                    ChangeTracker(app.database.syncQueueDao()).recordDelete(SyncQueueModel.TYPE_USER, userUuid)
                }
            }
            result
        }
    }

    fun renameLivreur(
        livreur: StockLivreur,
        name: String,
        phone: String,
        username: String,
        password: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Livreur mis à jour", onDone) {
            val result = repository.renameLivreur(livreur.id, name, phone, username, password.ifBlank { null })
            if (result is StockResult.Ok) updateFinanceUser(livreur.userUuid, username, phone, password)
            result
        }
    }

    fun deleteLivreur(livreur: StockLivreur, onDone: (Boolean) -> Unit = {}) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Livreur supprimé", onDone) {
            val result = repository.deleteLivreur(livreur.id)
            if (result is StockResult.Ok) deleteFinanceUser(livreur.userUuid)
            result
        }
    }

    fun giveMiniStock(
        livreurId: Long,
        productId: Long,
        quantityRaw: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Mini-stock remis au livreur", onDone) {
            repository.giveMiniStock(livreurId, productId, quantity, note)
        }
    }

    fun returnMiniStock(
        livreurId: Long,
        productId: Long,
        quantityRaw: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Retour enregistré au dépôt", onDone) {
            repository.returnMiniStock(livreurId, productId, quantity, note)
        }
    }

    fun recordSortie(
        livreurId: Long,
        productId: Long,
        quantityRaw: String,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        val current = sessionState.value
        if (current == null || (current.isLivreur && current.livreurId != livreurId)) {
            messages.tryEmit("Cette sortie ne vous est pas autorisée")
            onDone(false)
            return
        }
        launch("Sortie enregistrée", onDone) {
            repository.recordSortie(livreurId, productId, quantity, note)
        }
    }

    fun deliver(
        livreurId: Long,
        quantities: List<Pair<Long, String>>,
        orderId: Long?,
        note: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        val current = sessionState.value
        if (current == null || (current.isLivreur && current.livreurId != livreurId)) {
            messages.tryEmit("Cette livraison ne vous est pas autorisée")
            onDone(false)
            return
        }
        val lines = mutableListOf<DeliveryLine>()
        for ((productId, raw) in quantities) {
            if (raw.isBlank()) continue
            val quantity = StockRules.parseQuantity(raw)
            if (quantity == null) {
                messages.tryEmit("Quantité invalide")
                onDone(false)
                return
            }
            lines += DeliveryLine(productId, quantity)
        }
        if (lines.isEmpty()) {
            messages.tryEmit("Indiquez au moins une quantité à livrer")
            onDone(false)
            return
        }
        launch("Livraison enregistrée", onDone) {
            repository.deliver(current.organismId, livreurId, lines, orderId, note)
        }
    }

    fun createOrder(
        customer: String,
        livreurId: Long?,
        note: String,
        lines: List<DeliveryLine>,
        onDone: (Boolean) -> Unit = {}
    ) {
        val organism = ownerOrganism() ?: run { onDone(false); return }
        launch("Commande enregistrée", onDone) {
            repository.createOrder(organism, customer, livreurId, note, lines)
        }
    }

    fun cancelOrder(order: StockOrder, onDone: (Boolean) -> Unit = {}) {
        val organism = ownerOrganism() ?: run { onDone(false); return }
        launch("Commande annulée", onDone) { repository.cancelOrder(order.id, organism) }
    }

    fun miniQuantity(livreurId: Long, productId: Long, lines: List<MiniStockLine> = miniStocks.value): Double {
        return lines.firstOrNull { it.livreurId == livreurId && it.productId == productId }?.quantity ?: 0.0
    }

    fun distributedQuantity(productId: Long, lines: List<MiniStockLine> = miniStocks.value): Double {
        return lines.filter { it.productId == productId }.sumOf { it.quantity }
    }

    fun livreurTotal(livreurId: Long, lines: List<MiniStockLine> = miniStocks.value): Double {
        return lines.filter { it.livreurId == livreurId }.sumOf { it.quantity }
    }

    private fun ownerOrganism(): Long? {
        val current = sessionState.value
        if (current == null || !current.isOwner) {
            messages.tryEmit("Réservé à la gestion de l'organisation")
            return null
        }
        return current.organismId
    }

    private fun updateFinanceUser(userUuid: String, username: String, phone: String, password: String) {
        if (userUuid.isBlank()) return
        val login = StockRules.normalizeUsername(username) ?: return
        val app = getApplication<MCoreApplication>()
        val existing = app.database.userDao().findByUuid(userUuid) ?: return
        app.userRepository.update(
            existing.copy(
                username = login,
                phone = phone.trim().ifBlank { null },
                password = password.ifBlank { existing.password }
            )
        )
    }

    private fun deleteFinanceUser(userUuid: String) {
        if (userUuid.isBlank()) return
        val app = getApplication<MCoreApplication>()
        val existing = app.database.userDao().findByUuid(userUuid) ?: return
        app.database.userDao().delete(existing)
        ChangeTracker(app.database.syncQueueDao()).recordDelete(SyncQueueModel.TYPE_USER, userUuid)
    }

    private suspend fun publishCloud(): String? {
        val app = getApplication<MCoreApplication>()
        try {
            val session = app.sessionRepository.session.first()
            if (session.isLoggedIn && session.organismUuid.isNotBlank()) {
                app.syncManager.syncAll(session.organismUuid, session.username, session.role)
            }
        } catch (error: Exception) {
            Log.e(TAG, "Sync comptable impossible", error)
        }
        val uuid = sessionState.value?.organismUuid.orEmpty()
        return if (uuid.isBlank()) null else sync.sync(uuid)
    }

    private fun launch(success: String, onDone: (Boolean) -> Unit = {}, block: suspend () -> StockResult) {
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    StockResult.Ok -> {
                        messages.emit(success)
                        publishCloud()?.let { messages.emit("Enregistré ici. Cloud : $it") }
                        onDone(true)
                    }
                    is StockResult.Error -> {
                        messages.emit(result.message)
                        onDone(false)
                    }
                }
            } catch (_: Exception) {
                messages.emit("Opération impossible")
                onDone(false)
            }
        }
    }

    companion object {
        private const val TAG = "StockViewModel"
    }
}
