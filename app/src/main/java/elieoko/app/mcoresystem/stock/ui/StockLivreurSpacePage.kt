package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import elieoko.app.mcoresystem.stock.data.OrderStatus
import elieoko.app.mcoresystem.stock.data.StockProduct

@Composable
fun StockLivreurSpacePage(
    viewModel: StockViewModel,
    onLeave: () -> Unit,
    onLogout: () -> Unit,
    onDeliver: () -> Unit,
    onHistory: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val products by viewModel.products.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val livreurId = session?.livreurId ?: 0L
    val held = lines.filter { it.livreurId == livreurId && it.quantity > StockRules.EPSILON }
    val mine = orders.filter {
        it.livreurId == livreurId && it.status != OrderStatus.ANNULEE.name && it.status != OrderStatus.LIVREE.name
    }
    var pending by remember { mutableStateOf<StockProduct?>(null) }

    StockScaffold(
        title = session?.livreurName?.ifBlank { null } ?: "Mon mini-stock",
        onBack = onLeave,
        floatingAction = { StockFab(Icons.Default.LocalShipping, "Livraison", onDeliver) },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Déconnexion")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    session?.organismName ?: "Organisation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                InfoCard(
                    title = "Commandes à livrer",
                    value = mine.size.toString(),
                    subtitle = "Le bouton livraison enregistre les sorties",
                    onClick = onDeliver
                )
            }
            item { SectionLabel("Mes articles") }
            if (held.isEmpty()) {
                item { EmptyHint("Aucun article pour le moment. Le propriétaire doit vous remettre un mini-stock.") }
            }
            items(held, key = { it.id }) { line ->
                val product = products.firstOrNull { it.id == line.productId }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product?.name ?: "Article", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${StockRules.formatQty(line.quantity)} ${product?.unit ?: ""}".trim())
                        androidx.compose.material3.TextButton(onClick = { product?.let { pending = it } }) {
                            Text("Sortie unitaire")
                        }
                    }
                }
            }
            item {
                androidx.compose.material3.TextButton(onClick = onHistory) { Text("Mon historique") }
            }
        }
    }

    pending?.let { product ->
        var quantity by remember(product.id) { mutableStateOf("") }
        var note by remember(product.id) { mutableStateOf("") }
        QuantityDialog(
            title = "Sortie",
            hint = "${product.name} quitte votre mini-stock.",
            confirmLabel = "Enregistrer",
            quantity = quantity,
            note = note,
            onQuantity = { quantity = it },
            onNote = { note = it },
            onConfirm = {
                viewModel.recordSortie(livreurId, product.id, quantity, note) { ok -> if (ok) pending = null }
            },
            onDismiss = { pending = null }
        )
    }
}
