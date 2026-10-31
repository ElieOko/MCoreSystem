package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel

@Composable
fun StockAssignPage(
    viewModel: StockViewModel,
    initialLivreurId: Long,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val livreurs by viewModel.livreurs.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    var livreurId by remember { mutableLongStateOf(initialLivreurId) }
    var productId by remember { mutableLongStateOf(-1L) }
    var quantity by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pickingLivreur by remember { mutableStateOf(false) }
    var pickingProduct by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }

    val livreur = livreurs.firstOrNull { it.id == livreurId }
    val product = products.firstOrNull { it.id == productId }
    val already = if (livreur != null && product != null) {
        viewModel.miniQuantity(livreur.id, product.id, lines)
    } else {
        0.0
    }

    StockScaffold(title = "Donner un mini-stock", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "La quantité est retirée du dépôt et ajoutée au mini-stock du livreur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                PickerCard(
                    label = "Livreur",
                    value = livreur?.name ?: "Choisir un livreur",
                    onClick = { pickingLivreur = true }
                )
            }
            item {
                PickerCard(
                    label = "Article",
                    value = product?.let {
                        "${it.name} — dépôt ${StockRules.formatQty(it.centralQuantity)} ${it.unit}"
                    } ?: "Choisir un article",
                    onClick = { pickingProduct = true }
                )
            }
            if (livreur != null && product != null) {
                item {
                    Text(
                        "Déjà chez ${livreur.name} : ${StockRules.formatQty(already)} ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            item { LabeledField("Quantité à donner", quantity, { quantity = it }) }
            item { LabeledField("Note", note, { note = it }, "Facultatif") }
            item {
                Button(
                    onClick = {
                        if (livreur == null || product == null || sending) return@Button
                        sending = true
                        viewModel.giveMiniStock(livreur.id, product.id, quantity, note) { ok ->
                            sending = false
                            if (ok) {
                                quantity = ""
                                note = ""
                            }
                        }
                    },
                    enabled = !sending && livreur != null && product != null && quantity.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Remettre le mini-stock")
                }
            }
            if (livreurs.isEmpty() || products.isEmpty()) {
                item {
                    EmptyHint(
                        if (products.isEmpty()) {
                            "Ajoutez d'abord un article avec du stock au dépôt."
                        } else {
                            "Ajoutez d'abord un livreur."
                        }
                    )
                }
            }
        }
    }

    if (pickingLivreur) {
        ChoiceDialog(
            title = "Livreur",
            options = livreurs.map { it.id to it.name },
            empty = "Aucun livreur",
            onPick = {
                livreurId = it
                pickingLivreur = false
            },
            onDismiss = { pickingLivreur = false }
        )
    }
    if (pickingProduct) {
        ChoiceDialog(
            title = "Article du dépôt",
            options = products.map { productOption ->
                productOption.id to "${productOption.name} (${StockRules.formatQty(productOption.centralQuantity)} ${productOption.unit})"
            },
            empty = "Aucun article",
            onPick = {
                productId = it
                pickingProduct = false
            },
            onDismiss = { pickingProduct = false }
        )
    }
}

@Composable
private fun PickerCard(label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<Long, String>>,
    empty: String,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (options.isEmpty()) {
                    Text(empty)
                } else {
                    options.forEach { (id, label) ->
                        TextButton(onClick = { onPick(id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
