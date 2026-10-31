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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import elieoko.app.mcoresystem.stock.data.StockProduct

@Composable
fun StockProductsPage(viewModel: StockViewModel, onBack: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StockProduct?>(null) }
    var receiving by remember { mutableStateOf<StockProduct?>(null) }
    var withdrawing by remember { mutableStateOf<StockProduct?>(null) }
    var deleting by remember { mutableStateOf<StockProduct?>(null) }

    StockScaffold(
        title = "Articles",
        onBack = onBack,
        floatingAction = { StockFab(Icons.Default.Add, "Nouvel article") { creating = true } }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (products.isEmpty()) {
                item { EmptyHint("Aucun article. Ajoutez le stock du dépôt, puis donnez-en aux livreurs.") }
            }
            items(products, key = { it.id }) { product ->
                val distributed = viewModel.distributedQuantity(product.id, lines)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Dépôt : ${StockRules.formatQty(product.centralQuantity)} ${product.unit}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Montant : ${StockRules.formatAmount(product.unitPrice)} / ${product.unit} • valeur ${StockRules.formatAmount(product.centralQuantity * product.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Chez les livreurs : ${StockRules.formatQty(distributed)} ${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { receiving = product }) { Text("Entrée") }
                            TextButton(onClick = { withdrawing = product }) { Text("Retrait") }
                            IconButton(onClick = { editing = product }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier")
                            }
                            IconButton(onClick = { deleting = product }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        ProductFormDialog(
            title = "Nouvel article",
            name = "",
            unit = "pièce",
            showInitial = true,
            onDismiss = { creating = false },
            onSave = { name, unit, initial, price ->
                viewModel.createProduct(name, unit, initial, price) { ok -> if (ok) creating = false }
            }
        )
    }
    editing?.let { product ->
        ProductFormDialog(
            title = "Modifier l'article",
            name = product.name,
            unit = product.unit,
            price = StockRules.formatAmount(product.unitPrice),
            showInitial = false,
            onDismiss = { editing = null },
            onSave = { name, unit, _, price ->
                viewModel.renameProduct(product, name, unit, price) { ok -> if (ok) editing = null }
            }
        )
    }
    receiving?.let { product ->
        var quantity by remember(product.id) { mutableStateOf("") }
        var note by remember(product.id) { mutableStateOf("") }
        QuantityDialog(
            title = "Entrée dépôt",
            hint = "${product.name} — dépôt actuel : ${StockRules.formatQty(product.centralQuantity)} ${product.unit}",
            confirmLabel = "Enregistrer",
            quantity = quantity,
            note = note,
            onQuantity = { quantity = it },
            onNote = { note = it },
            onConfirm = {
                viewModel.receiveStock(product, quantity, note) { ok -> if (ok) receiving = null }
            },
            onDismiss = { receiving = null }
        )
    }
    withdrawing?.let { product ->
        var quantity by remember(product.id) { mutableStateOf("") }
        var note by remember(product.id) { mutableStateOf("") }
        QuantityDialog(
            title = "Retrait dépôt",
            hint = "${product.name} — dépôt actuel : ${StockRules.formatQty(product.centralQuantity)} ${product.unit}. Le retrait ne touche pas les mini-stocks.",
            confirmLabel = "Retirer",
            quantity = quantity,
            note = note,
            onQuantity = { quantity = it },
            onNote = { note = it },
            onConfirm = {
                viewModel.withdrawStock(product, quantity, note) { ok -> if (ok) withdrawing = null }
            },
            onDismiss = { withdrawing = null }
        )
    }
    deleting?.let { product ->
        ConfirmDialog(
            title = "Supprimer l'article",
            message = "« ${product.name} » sera retiré. La suppression est refusée s'il reste du stock au dépôt ou chez un livreur.",
            onConfirm = {
                viewModel.deleteProduct(product) { ok -> if (ok) deleting = null }
            },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ProductFormDialog(
    title: String,
    name: String,
    unit: String,
    price: String = "",
    showInitial: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var nameValue by remember { mutableStateOf(name) }
    var unitValue by remember { mutableStateOf(unit) }
    var priceValue by remember { mutableStateOf(price) }
    var initial by remember { mutableStateOf("") }
    FormDialog(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onSave(nameValue, unitValue, initial, priceValue) }
    ) {
        LabeledField("Nom", nameValue, { nameValue = it }, "Ex. Pain, Eau")
        LabeledField("Unité", unitValue, { unitValue = it }, "pièce, carton, kg")
        LabeledField("Prix unitaire", priceValue, { priceValue = it }, "0", KeyboardType.Decimal)
        if (showInitial) {
            LabeledField("Quantité initiale au dépôt", initial, { initial = it }, "0", KeyboardType.Decimal)
        }
    }
}
