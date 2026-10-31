package elieoko.app.mcoresystem.stock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import elieoko.app.mcoresystem.stock.data.MiniStockLine
import elieoko.app.mcoresystem.stock.data.StockDatabase
import elieoko.app.mcoresystem.stock.data.StockLivreur
import elieoko.app.mcoresystem.stock.data.StockProduct
import elieoko.app.mcoresystem.stock.data.StockRepository
import elieoko.app.mcoresystem.stock.data.StockResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StockRepository(StockDatabase.get(application))
    private val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val message = messages.asSharedFlow()
    val products = repository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val livreurs = repository.observeLivreurs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val miniStocks = repository.observeMiniStocks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val movements = repository.observeMovements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createProduct(name: String, unit: String, initialQuantityRaw: String, onDone: (Boolean) -> Unit = {}) {
        val quantity = if (initialQuantityRaw.isBlank()) 0.0 else StockRules.parseQuantity(initialQuantityRaw)
        if (initialQuantityRaw.isNotBlank() && quantity == null) {
            messages.tryEmit("Quantité initiale invalide")
            onDone(false)
            return
        }
        launch("Article enregistré", onDone) { repository.createProduct(name, unit, quantity ?: 0.0) }
    }

    fun renameProduct(product: StockProduct, name: String, unit: String, onDone: (Boolean) -> Unit = {}) {
        launch("Article mis à jour", onDone) { repository.renameProduct(product.id, name, unit) }
    }

    fun deleteProduct(product: StockProduct, onDone: (Boolean) -> Unit = {}) {
        launch("Article supprimé", onDone) { repository.deleteProduct(product.id) }
    }

    fun receiveStock(product: StockProduct, quantityRaw: String, note: String, onDone: (Boolean) -> Unit = {}) {
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        launch("Entrée enregistrée au dépôt", onDone) { repository.receiveStock(product.id, quantity, note) }
    }

    fun withdrawStock(product: StockProduct, quantityRaw: String, note: String, onDone: (Boolean) -> Unit = {}) {
        val quantity = StockRules.parseQuantity(quantityRaw)
        if (quantity == null) {
            messages.tryEmit("Quantité invalide")
            onDone(false)
            return
        }
        launch("Retrait enregistré", onDone) { repository.withdrawStock(product.id, quantity, note) }
    }

    fun createLivreur(name: String, phone: String, onDone: (Boolean) -> Unit = {}) {
        launch("Livreur enregistré", onDone) { repository.createLivreur(name, phone) }
    }

    fun renameLivreur(livreur: StockLivreur, name: String, phone: String, onDone: (Boolean) -> Unit = {}) {
        launch("Livreur mis à jour", onDone) { repository.renameLivreur(livreur.id, name, phone) }
    }

    fun deleteLivreur(livreur: StockLivreur, onDone: (Boolean) -> Unit = {}) {
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
        launch("Sortie enregistrée", onDone) {
            repository.recordSortie(livreurId, productId, quantity, note)
        }
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
