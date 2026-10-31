package elieoko.app.mcoresystem.stock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.stock.data.DeliveryLine
import elieoko.app.mcoresystem.stock.data.MiniStockLine
import elieoko.app.mcoresystem.stock.data.StockAuth
import elieoko.app.mcoresystem.stock.data.StockDatabase
import elieoko.app.mcoresystem.stock.data.StockLivreur
import elieoko.app.mcoresystem.stock.data.StockOrder
import elieoko.app.mcoresystem.stock.data.StockProduct
import elieoko.app.mcoresystem.stock.data.StockRepository
import elieoko.app.mcoresystem.stock.data.StockResult
import elieoko.app.mcoresystem.stock.data.StockRole
import elieoko.app.mcoresystem.stock.data.StockSession
import elieoko.app.mcoresystem.stock.data.StockSessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository(StockDatabase.get(application))
    private val sessionStore = StockSessionStore(application)
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

    init {
        viewModelScope.launch {
            sessionStore.session.collect { saved ->
                sessionState.value = saved
                readyState.value = true
            }
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
        viewModelScope.launch { sessionStore.clear() }
    }

    fun createProduct(name: String, unit: String, initialQuantityRaw: String, onDone: (Boolean) -> Unit = {}) {
        val organism = ownerOrganism() ?: run { onDone(false); return }
        val quantity = if (initialQuantityRaw.isBlank()) 0.0 else StockRules.parseQuantity(initialQuantityRaw)
        if (initialQuantityRaw.isNotBlank() && quantity == null) {
            messages.tryEmit("Quantité initiale invalide")
            onDone(false)
            return
        }
        launch("Article enregistré", onDone) {
            repository.createProduct(organism, name, unit, quantity ?: 0.0)
        }
    }

    fun renameProduct(product: StockProduct, name: String, unit: String, onDone: (Boolean) -> Unit = {}) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Article mis à jour", onDone) { repository.renameProduct(product.id, name, unit) }
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
        launch("Livreur enregistré. Il peut se connecter avec son identifiant.", onDone) {
            repository.createLivreur(organism, name, phone, username, password)
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
            repository.renameLivreur(livreur.id, name, phone, username, password.ifBlank { null })
        }
    }

    fun deleteLivreur(livreur: StockLivreur, onDone: (Boolean) -> Unit = {}) {
        if (ownerOrganism() == null) {
            onDone(false)
            return
        }
        launch("Livreur supprimé", onDone) { repository.deleteLivreur(livreur.id) }
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
        if (current == null || current.role != StockRole.OWNER.name) {
            messages.tryEmit("Réservé au propriétaire de l'organisation")
            return null
        }
        return current.organismId
    }

    private fun launch(success: String, onDone: (Boolean) -> Unit = {}, block: suspend () -> StockResult) {
        viewModelScope.launch {
            try {
                when (val result = block()) {
                    StockResult.Ok -> {
                        messages.emit(success)
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
}
