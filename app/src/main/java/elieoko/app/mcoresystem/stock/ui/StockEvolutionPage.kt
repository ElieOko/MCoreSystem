package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import elieoko.app.mcoresystem.stock.data.MovementType

@Composable
fun StockEvolutionPage(viewModel: StockViewModel, onBack: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    val movements by viewModel.movements.collectAsState()

    StockScaffold(title = "Évolution des articles", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Le propriétaire voit le dépôt, le mini-stock encore chez les livreurs, et le cumul des mouvements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (products.isEmpty()) {
                item { EmptyHint("Aucun article dans cette organisation.") }
            }
            items(products, key = { it.id }) { product ->
                val productMoves = movements.filter { it.productId == product.id }
                fun total(type: MovementType) = productMoves.filter { it.type == type.name }.sumOf { it.quantity }
                val atLivreurs = lines.filter { it.productId == product.id }.sumOf { it.quantity }
                InfoCard(
                    title = product.name,
                    value = "Dépôt ${StockRules.formatQty(product.centralQuantity)} ${product.unit}",
                    subtitle = "Chez les livreurs ${StockRules.formatQty(atLivreurs)} • " +
                        "entrées ${StockRules.formatQty(total(MovementType.ENTREE))} • " +
                        "dotations ${StockRules.formatQty(total(MovementType.DOTATION))} • " +
                        "sorties ${StockRules.formatQty(total(MovementType.SORTIE))} • " +
                        "retours ${StockRules.formatQty(total(MovementType.RETOUR))}"
                )
            }
        }
    }
}
