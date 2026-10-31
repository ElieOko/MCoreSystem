package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import elieoko.app.mcoresystem.stock.data.OrderStatus

@Composable
fun StockDeliveryPage(viewModel: StockViewModel, livreurId: Long, onBack: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val orderLines by viewModel.orderLines.collectAsState()
    val held = lines.filter { it.livreurId == livreurId && it.quantity > StockRules.EPSILON }
    val openOrders = orders.filter {
        it.livreurId == livreurId &&
            it.status != OrderStatus.ANNULEE.name &&
            it.status != OrderStatus.LIVREE.name
    }
    var orderId by remember { mutableLongStateOf(-1L) }
    var note by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    val quantities = remember { mutableStateMapOf<Long, String>() }
    val selected = openOrders.firstOrNull { it.id == orderId }

    StockScaffold(title = "Livraison", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Saisissez les quantités qui sortent de votre mini-stock pour cette livraison.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                TextButton(onClick = { picking = true }) {
                    Text(selected?.let { "Commande : ${it.customerName}" } ?: "Livraison libre (sans commande)")
                }
            }
            if (selected != null) {
                item {
                    val expected = orderLines.filter { it.orderId == selected.id }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("À livrer", fontWeight = FontWeight.SemiBold)
                        expected.forEach { line ->
                            val product = products.firstOrNull { it.id == line.productId }
                            val left = (line.quantityOrdered - line.quantityDelivered).coerceAtLeast(0.0)
                            Text(
                                "${product?.name ?: "Article"} : ${StockRules.formatQty(left)} restant(s)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            if (held.isEmpty()) {
                item { EmptyHint("Vous n'avez aucun article à sortir.") }
            }
            items(held.size) { index ->
                val line = held[index]
                val product = products.firstOrNull { it.id == line.productId }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${product?.name ?: "Article"} • disponible ${StockRules.formatQty(line.quantity)} ${product?.unit ?: ""}".trim(),
                        fontWeight = FontWeight.Medium
                    )
                    LabeledField(
                        "Quantité sortie",
                        quantities[line.productId].orEmpty(),
                        { quantities[line.productId] = it },
                        "0"
                    )
                }
            }
            item { LabeledField("Note", note, { note = it }, "Client, adresse…") }
            item {
                Button(
                    onClick = {
                        sending = true
                        viewModel.deliver(
                            livreurId = livreurId,
                            quantities = held.map { it.productId to quantities[it.productId].orEmpty() },
                            orderId = orderId.takeIf { it > 0 },
                            note = note
                        ) { ok ->
                            sending = false
                            if (ok) onBack()
                        }
                    },
                    enabled = !sending && held.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) { Text("Enregistrer la sortie") }
            }
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Commande") },
            text = {
                Column {
                    TextButton(onClick = {
                        orderId = -1L
                        picking = false
                    }) { Text("Livraison libre") }
                    openOrders.forEach { order ->
                        TextButton(onClick = {
                            orderId = order.id
                            picking = false
                        }) { Text("${order.customerName} • ${OrderStatus.from(order.status).label}") }
                    }
                    if (openOrders.isEmpty()) Text("Aucune commande ouverte ne vous est assignée.")
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Fermer") } }
        )
    }
}
