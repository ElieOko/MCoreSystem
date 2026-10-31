package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockAssignPage(
    viewModel: StockViewModel,
    initialLivreurId: Long,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val livreurs by viewModel.livreurs.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    var livreurId by remember(initialLivreurId) { mutableLongStateOf(initialLivreurId) }
    var productId by remember { mutableLongStateOf(-1L) }
    var quantity by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    val livreur = livreurs.firstOrNull { it.id == livreurId }
    val product = products.firstOrNull { it.id == productId }
    val already = if (livreur != null && product != null) {
        viewModel.miniQuantity(livreur.id, product.id, lines)
    } else {
        0.0
    }
    val parsed = StockRules.parseQuantity(quantity)
    val lineValue = if (product != null && parsed != null) parsed * product.unitPrice else null

    StockScaffold(title = "Ajouter au mini-stock", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Choisissez le livreur et l'article. La quantité quitte le dépôt et rejoint son mini-stock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text("Livreur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (livreurs.isEmpty()) {
                item { EmptyHint("Ajoutez d'abord un livreur.") }
            } else {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        livreurs.forEach { candidate ->
                            FilterChip(
                                selected = candidate.id == livreurId,
                                onClick = { livreurId = candidate.id },
                                label = { Text(candidate.name) }
                            )
                        }
                    }
                }
            }
            item {
                Text("Article du dépôt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (products.isEmpty()) {
                item { EmptyHint("Ajoutez d'abord un article avec du stock au dépôt.") }
            }
            items(products, key = { it.id }) { candidate ->
                val held = if (livreur == null) 0.0 else viewModel.miniQuantity(livreur.id, candidate.id, lines)
                val selected = candidate.id == productId
                Card(
                    onClick = {
                        productId = candidate.id
                        quantity = ""
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = if (selected) BorderStroke(2.dp, AccentTeal) else null,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(candidate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Dépôt : ${StockRules.formatQty(candidate.centralQuantity)} ${candidate.unit} • ${StockRules.formatAmount(candidate.unitPrice)} / ${candidate.unit}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Déjà chez ${livreur?.name ?: "le livreur"} : ${StockRules.formatQty(held)} ${candidate.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selected) {
                            LabeledField(
                                "Quantité à ajouter",
                                quantity,
                                { quantity = it },
                                "0",
                                KeyboardType.Decimal
                            )
                            LabeledField("Note", note, { note = it }, "Facultatif")
                            if (lineValue != null) {
                                Text(
                                    "Valeur remise : ${StockRules.formatAmount(lineValue)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentTeal
                                )
                            }
                        }
                    }
                }
            }
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
                    Text(if (livreur == null) "Choisir un livreur" else "Ajouter au mini-stock")
                }
            }
        }
    }
}
