package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import elieoko.app.mcoresystem.stock.data.StockProduct

private enum class MiniAction { RETOUR, SORTIE }

@Composable
fun StockLivreurDetailPage(
    viewModel: StockViewModel,
    livreurId: Long,
    onBack: () -> Unit,
    onGiveMore: () -> Unit
) {
    val livreurs by viewModel.livreurs.collectAsState()
    val products by viewModel.products.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    val livreur = livreurs.firstOrNull { it.id == livreurId }
    val held = lines.filter { it.livreurId == livreurId && it.quantity > StockRules.EPSILON }
    val heldValue = held.sumOf { line ->
        val price = products.firstOrNull { it.id == line.productId }?.unitPrice ?: 0.0
        line.quantity * price
    }
    var pending by remember { mutableStateOf<Pair<StockProduct, MiniAction>?>(null) }

    StockScaffold(
        title = livreur?.name ?: "Livreur",
        onBack = onBack,
        floatingAction = { StockFab(Icons.Default.Add, "Donner du stock", onGiveMore) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                InfoCard(
                    title = livreur?.phone?.takeIf { it.isNotBlank() } ?: "Mini-stock détenu",
                    value = "${StockRules.formatQty(held.sumOf { it.quantity })} unités",
                    subtitle = "${held.size} article(s) • ${StockRules.formatAmount(heldValue)}",
                    onClick = onGiveMore
                )
            }
            if (held.isEmpty()) {
                item { EmptyHint("Ce livreur n'a pas de mini-stock. Utilisez + pour lui en remettre.") }
            }
            items(held, key = { it.id }) { line ->
                val product = products.firstOrNull { it.id == line.productId }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            product?.name ?: "Article",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${StockRules.formatQty(line.quantity)} ${product?.unit ?: ""}".trim(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        val price = product?.unitPrice ?: 0.0
                        Text(
                            "${StockRules.formatAmount(price)} / ${product?.unit ?: "unité"} • valeur ${StockRules.formatAmount(line.quantity * price)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { product?.let { pending = it to MiniAction.RETOUR } }) {
                                Text("Retour dépôt")
                            }
                            OutlinedButton(onClick = { product?.let { pending = it to MiniAction.SORTIE } }) {
                                Text("Sortie")
                            }
                        }
                    }
                }
            }
        }
    }

    pending?.let { (product, action) ->
        var quantity by remember(product.id, action) { mutableStateOf("") }
        var note by remember(product.id, action) { mutableStateOf("") }
        val available = viewModel.miniQuantity(livreurId, product.id, lines)
        val isReturn = action == MiniAction.RETOUR
        QuantityDialog(
            title = if (isReturn) "Retour au dépôt" else "Sortie du mini-stock",
            hint = if (isReturn) {
                "${product.name} revient au dépôt. Disponible : ${StockRules.formatQty(available)} ${product.unit}"
            } else {
                "${product.name} quitte le mini-stock sans revenir au dépôt. Disponible : ${StockRules.formatQty(available)} ${product.unit}"
            },
            confirmLabel = if (isReturn) "Retourner" else "Enregistrer la sortie",
            quantity = quantity,
            note = note,
            onQuantity = { quantity = it },
            onNote = { note = it },
            onConfirm = {
                val done: (Boolean) -> Unit = { ok -> if (ok) pending = null }
                if (isReturn) viewModel.returnMiniStock(livreurId, product.id, quantity, note, done)
                else viewModel.recordSortie(livreurId, product.id, quantity, note, done)
            },
            onDismiss = { pending = null }
        )
    }
}
