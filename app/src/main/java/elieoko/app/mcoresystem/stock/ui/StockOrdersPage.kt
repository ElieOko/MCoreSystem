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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import elieoko.app.mcoresystem.stock.data.DeliveryLine
import elieoko.app.mcoresystem.stock.data.OrderStatus

@Composable
fun StockOrdersPage(viewModel: StockViewModel, onBack: () -> Unit) {
    val orders by viewModel.orders.collectAsState()
    val lines by viewModel.orderLines.collectAsState()
    val products by viewModel.products.collectAsState()
    val livreurs by viewModel.livreurs.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var cancelling by remember { mutableLongStateOf(-1L) }

    StockScaffold(
        title = "Commandes",
        onBack = onBack,
        floatingAction = { StockFab(Icons.Default.Add, "Nouvelle commande") { creating = true } }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (orders.isEmpty()) {
                item { EmptyHint("Aucune commande. Ajoutez-en une, puis assignez-la à un livreur.") }
            }
            items(orders, key = { it.id }) { order ->
                val orderLines = lines.filter { it.orderId == order.id }
                val livreur = livreurs.firstOrNull { it.id == order.livreurId }
                val ordered = orderLines.sumOf { it.quantityOrdered }
                val delivered = orderLines.sumOf { it.quantityDelivered }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(order.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        TypeBadge(OrderStatus.from(order.status).label, MaterialTheme.colorScheme.primary)
                        Text(
                            livreur?.let { "Livreur : ${it.name}" } ?: "Pas encore assignée",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("${StockRules.formatQty(delivered)} / ${StockRules.formatQty(ordered)} livré(s)")
                        orderLines.forEach { line ->
                            val product = products.firstOrNull { it.id == line.productId }
                            Text(
                                "${product?.name ?: "Article"} : ${StockRules.formatQty(line.quantityDelivered)} / ${StockRules.formatQty(line.quantityOrdered)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (order.note.isNotBlank()) {
                            Text(order.note, style = MaterialTheme.typography.bodySmall)
                        }
                        if (order.status != OrderStatus.LIVREE.name && order.status != OrderStatus.ANNULEE.name) {
                            TextButton(onClick = { cancelling = order.id }) { Text("Annuler") }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        OrderEditor(viewModel, onDismiss = { creating = false })
    }
    if (cancelling > 0) {
        val order = orders.firstOrNull { it.id == cancelling }
        if (order != null) {
            ConfirmDialog(
                title = "Annuler la commande",
                message = "« ${order.customerName} » sera annulée. Les sorties déjà enregistrées restent dans l'historique.",
                confirmLabel = "Annuler la commande",
                onConfirm = { viewModel.cancelOrder(order) { ok -> if (ok) cancelling = -1L } },
                onDismiss = { cancelling = -1L }
            )
        }
    }
}

@Composable
private fun OrderEditor(viewModel: StockViewModel, onDismiss: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val livreurs by viewModel.livreurs.collectAsState()
    var customer by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var livreurId by remember { mutableLongStateOf(-1L) }
    var productId by remember { mutableLongStateOf(-1L) }
    var quantity by remember { mutableStateOf("") }
    var pickingLivreur by remember { mutableStateOf(false) }
    var pickingProduct by remember { mutableStateOf(false) }
    val draft = remember { androidx.compose.runtime.mutableStateListOf<DeliveryLine>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle commande") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Client", customer, { customer = it })
                TextButton(onClick = { pickingLivreur = true }) {
                    val name = livreurs.firstOrNull { it.id == livreurId }?.name
                    Text(name?.let { "Livreur : $it" } ?: "Assigner un livreur (facultatif)")
                }
                TextButton(onClick = { pickingProduct = true }) {
                    val name = products.firstOrNull { it.id == productId }?.name
                    Text(name ?: "Choisir un article")
                }
                LabeledField("Quantité commandée", quantity, { quantity = it })
                TextButton(onClick = {
                    val parsed = StockRules.parseQuantity(quantity) ?: return@TextButton
                    if (productId <= 0) return@TextButton
                    val index = draft.indexOfFirst { it.productId == productId }
                    if (index >= 0) {
                        draft[index] = DeliveryLine(productId, draft[index].quantity + parsed)
                    } else {
                        draft.add(DeliveryLine(productId, parsed))
                    }
                    quantity = ""
                }) { Text("Ajouter la ligne") }
                draft.forEach { line ->
                    val name = products.firstOrNull { it.id == line.productId }?.name ?: "Article"
                    Text("$name • ${StockRules.formatQty(line.quantity)}")
                }
                LabeledField("Note", note, { note = it }, "Facultatif")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.createOrder(
                    customer = customer,
                    livreurId = livreurId.takeIf { it > 0 },
                    note = note,
                    lines = draft.toList()
                ) { ok -> if (ok) onDismiss() }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )

    if (pickingLivreur) {
        ChoiceList(
            title = "Livreur",
            options = listOf(-1L to "Aucun") + livreurs.map { it.id to it.name },
            onPick = { livreurId = it; pickingLivreur = false },
            onDismiss = { pickingLivreur = false }
        )
    }
    if (pickingProduct) {
        ChoiceList(
            title = "Article",
            options = products.map { it.id to it.name },
            onPick = { productId = it; pickingProduct = false },
            onDismiss = { pickingProduct = false }
        )
    }
}

@Composable
private fun ChoiceList(
    title: String,
    options: List<Pair<Long, String>>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (options.isEmpty()) Text("Aucun élément")
                options.forEach { (id, label) ->
                    TextButton(onClick = { onPick(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
