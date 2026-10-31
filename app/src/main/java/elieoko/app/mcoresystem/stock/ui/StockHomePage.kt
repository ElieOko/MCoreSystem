package elieoko.app.mcoresystem.stock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import elieoko.app.mcoresystem.presentation.components.element.BalanceCard
import elieoko.app.mcoresystem.presentation.ui.theme.AccentTeal
import elieoko.app.mcoresystem.presentation.ui.theme.AccentViolet
import elieoko.app.mcoresystem.presentation.ui.theme.BluePrimary
import elieoko.app.mcoresystem.stock.StockRules
import elieoko.app.mcoresystem.stock.StockViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StockHomePage(
    viewModel: StockViewModel,
    onLeave: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenLivreurs: () -> Unit,
    onGive: () -> Unit,
    onOpenLivreur: (Long) -> Unit,
    onOpenMovements: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val livreurs by viewModel.livreurs.collectAsState()
    val lines by viewModel.miniStocks.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())
    val centralUnits = products.sumOf { it.centralQuantity }
    val miniUnits = lines.sumOf { it.quantity }
    val todayCount = movements.count { it.createdOn.startsWith(today) }

    StockScaffold(
        title = "Stock livreurs",
        onBack = onLeave,
        floatingAction = { StockFab(Icons.Default.Add, "Donner un mini-stock", onGive) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Dépôt et mini-stocks remis aux livreurs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                BalanceCard(
                    label = "Stock central",
                    amount = "${StockRules.formatQty(centralUnits)} unités",
                    subtitle = "${products.size} article(s) • ${StockRules.formatQty(miniUnits)} en mini-stock",
                    gradient = listOf(AccentTeal, ColorTealDark),
                    icon = Icons.Default.Inventory2
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard(
                        title = "Livreurs",
                        value = livreurs.size.toString(),
                        accent = BluePrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLivreurs
                    )
                    InfoCard(
                        title = "Aujourd'hui",
                        value = todayCount.toString(),
                        subtitle = "mouvement(s)",
                        accent = AccentViolet,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenMovements
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile("Articles", Icons.Default.Inventory2, Modifier.weight(1f), onOpenProducts)
                    QuickTile("Livreurs", Icons.Default.People, Modifier.weight(1f), onOpenLivreurs)
                    QuickTile("Historique", Icons.Default.History, Modifier.weight(1f), onOpenMovements)
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                SectionLabel("Mini-stock par livreur")
            }
            if (livreurs.isEmpty()) {
                item { EmptyHint("Ajoutez un livreur, puis remettez-lui un mini-stock depuis le bouton +.") }
            } else {
                items(livreurs, key = { it.id }) { livreur ->
                    val total = viewModel.livreurTotal(livreur.id, lines)
                    val kinds = lines.count { it.livreurId == livreur.id && it.quantity > 0.0 }
                    InfoCard(
                        title = livreur.name,
                        value = "${StockRules.formatQty(total)} unités",
                        subtitle = if (livreur.phone.isBlank()) "$kinds article(s)" else "${livreur.phone} • $kinds article(s)",
                        onClick = { onOpenLivreur(livreur.id) }
                    )
                }
            }
        }
    }
}

private val ColorTealDark = androidx.compose.ui.graphics.Color(0xFF00796B)

@Composable
private fun QuickTile(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = AccentTeal)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}
